// 用 HBuilderX 自带的 TypeScript 对被改动的 .uvue/.uts 做语法体检
// 说明：这只是「语法」层校验（uni-app x 的完整编译需要 HBuilderX/真机），
//       但能兜住括号不配对、少逗号、非法语句等低级错误，避免用户白等一次编译。
const fs = require('fs');
const path = require('path');
const ts = require('typescript');

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

function extractScript(src, ext) {
  if (ext === '.uts') return src;
  const m = src.match(/<script[^>]*>([\s\S]*?)<\/script>/);
  return m ? m[1] : '';
}

let bad = 0;
for (const rel of FILES) {
  const full = path.join(ROOT, rel);
  if (!fs.existsSync(full)) {
    console.log('MISS ' + rel + '  (文件不存在)');
    bad++;
    continue;
  }
  const code = extractScript(fs.readFileSync(full, 'utf8'), path.extname(rel));
  const out = ts.transpileModule(code, {
    compilerOptions: { target: ts.ScriptTarget.ESNext, module: ts.ModuleKind.ESNext },
    reportDiagnostics: true,
    fileName: rel,
  });
  const diags = (out.diagnostics || []).filter((d) => d.category === ts.DiagnosticCategory.Error);
  if (diags.length === 0) {
    console.log('OK   ' + rel);
    continue;
  }
  bad++;
  console.log('FAIL ' + rel);
  for (const d of diags) {
    const pos = d.file && d.start != null ? ts.getLineAndCharacterOfPosition(d.file, d.start) : null;
    console.log('      第 ' + (pos ? pos.line + 1 : '?') + ' 行: ' + ts.flattenDiagnosticMessageText(d.messageText, ' '));
  }
}
console.log('');
console.log(bad === 0 ? '✅ 语法检查全部通过（' + FILES.length + ' 个文件）' : '❌ ' + bad + ' 个文件存在问题');
process.exit(bad === 0 ? 0 : 1);
