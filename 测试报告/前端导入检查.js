// 检查被改动文件的导入是否真实存在（路径 + 具名导出），以及页面生命周期钩子是否已导入。
// 目的：兜住「import 名字写错 / 路径写错 / 用了 onLoad 但忘记 import」这类会让 HBuilderX 编译失败的问题。
const fs = require('fs');
const path = require('path');

const ROOT = 'D:/file/Shequ/app/frontend/uniappx/uniappx';

const FILES = [
  'App.uvue',
  'utils/analytics.uts',
  'pages/message/index.uvue',
  'pages/message/chat.uvue',
  'pages/my/index.uvue',
  'pages/my/contact-admin.uvue',
  'pages/forum/index.uvue',
  'pages/forum/detail.uvue',
  'pages/forum/publish.uvue',
  'pages/trade/index.uvue',
  'pages/trade/detail.uvue',
  'pages/trade/publish.uvue',
  'pages/task/index.uvue',
  'pages/task/detail.uvue',
];

// 页面级生命周期钩子（必须从 @dcloudio/uni-app 导入；App.uvue 的 App 级钩子除外）
const PAGE_HOOKS = ['onLoad', 'onShow', 'onHide', 'onUnload', 'onReady', 'onPullDownRefresh', 'onReachBottom', 'onBackPress'];

const problems = [];

function readCode(file) {
  const src = fs.readFileSync(file, 'utf8');
  if (path.extname(file) === '.uts') return src;
  const m = src.match(/<script[^>]*>([\s\S]*?)<\/script>/);
  return m ? m[1] : '';
}

/** 收集 .uts 文件导出的名字 */
function exportedNames(file) {
  const code = fs.readFileSync(file, 'utf8');
  const names = new Set();
  const re = /export\s+(?:function|const|let|var|class|type|interface|enum)\s+([A-Za-z0-9_$]+)/g;
  let m;
  while ((m = re.exec(code)) !== null) names.add(m[1]);
  // export { a, b }
  const re2 = /export\s*\{([^}]*)\}/g;
  while ((m = re2.exec(code)) !== null) {
    for (const part of m[1].split(',')) {
      const nm = part.trim().split(/\s+as\s+/).pop().trim();
      if (nm) names.add(nm);
    }
  }
  return names;
}

for (const rel of FILES) {
  const file = path.join(ROOT, rel);
  if (!fs.existsSync(file)) { problems.push(rel + ' : 文件不存在'); continue; }
  const code = readCode(file);
  const imported = new Set();

  const importRe = /import\s+(?:type\s+)?\{([^}]*)\}\s+from\s+'([^']+)'/g;
  let m;
  while ((m = importRe.exec(code)) !== null) {
    const names = m[1].split(',').map((s) => s.trim().split(/\s+as\s+/)[0].trim()).filter(Boolean);
    names.forEach((n) => imported.add(n));
    const spec = m[2];
    if (spec.startsWith('@') || spec === 'vue') continue; // 框架包跳过

    // 解析相对路径
    let target = path.resolve(path.dirname(file), spec);
    if (!fs.existsSync(target)) {
      if (fs.existsSync(target + '.uts')) target = target + '.uts';
      else if (fs.existsSync(target + '.ts')) target = target + '.ts';
      else { problems.push(`${rel} : 导入路径不存在 -> ${spec}`); continue; }
    }
    if (path.extname(target) === '.uts') {
      const ex = exportedNames(target);
      for (const n of names) {
        if (!ex.has(n)) problems.push(`${rel} : ${spec} 未导出 "${n}"（已导出：${[...ex].join(', ')}）`);
      }
    }
  }

  // 生命周期钩子使用检查（App.uvue 用 App 级钩子，豁免）
  if (path.extname(file) !== '.uts' && path.basename(file) !== 'App.uvue') {
    for (const hook of PAGE_HOOKS) {
      const used = new RegExp('(^|[^.\\w])' + hook + '\\s*\\(').test(code);
      if (used && !imported.has(hook)) {
        problems.push(`${rel} : 使用了 ${hook}( ) 但没有从 '@dcloudio/uni-app' 导入`);
      }
    }
  }
}

if (problems.length === 0) {
  console.log('✅ 导入与生命周期检查全部通过（' + FILES.length + ' 个文件）');
  process.exit(0);
}
console.log('❌ 发现问题 ' + problems.length + ' 处：');
for (const p of problems) console.log('   - ' + p);
process.exit(1);
