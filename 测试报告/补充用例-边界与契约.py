# -*- coding: utf-8 -*-
"""补充用例：边界 / 反向 / 契约一致性（黑盒冒烟第二轮）"""
import json
import random
import re
import requests
import redis

BASE = "http://127.0.0.1:8080/api/v1"
ADMIN = "http://127.0.0.1:8080/api/admin/v1"
R = redis.Redis(host="127.0.0.1", port=6379, db=0, decode_responses=True)
RESULTS = []


def rec(module, case, expect, actual, ok, note=""):
    RESULTS.append({"module": module, "case": case, "expect": expect, "actual": actual, "ok": ok, "note": note})
    flag = "PASS" if ok is True else ("WARN" if ok is None else "FAIL")
    print(f"[{flag}] {module} | {case} | 期望={expect} 实际={actual} {note}")


def api(method, path, token=None, body=None, params=None, auth=True, base=BASE):
    h = {"Content-Type": "application/json"}
    if auth and token:
        h["Authorization"] = "Bearer " + token
    try:
        resp = requests.request(method, base + path, headers=h, json=body, params=params, timeout=10)
    except Exception as e:
        return {"__error__": str(e), "__status__": -1}
    try:
        j = resp.json()
    except Exception:
        j = {"__raw__": resp.text[:200]}
    j["__status__"] = resp.status_code
    return j


def code_of(j):
    return j.get("code") if isinstance(j, dict) else None


def rand_mobile():
    return "139" + "".join(random.choice("0123456789") for _ in range(8))


def get_captcha():
    j = api("GET", "/auth/captcha", auth=False)
    if code_of(j) != 0:
        return None, None
    cid = j["data"]["captchaId"]
    return cid, R.get("captcha:" + cid)


def seed(scene, mobile, code="888888"):
    R.set(f"sms:code:{scene}:{mobile}", code, ex=300)
    R.delete("sms:limit:min:" + mobile)
    return code


def register(nickname):
    mobile = rand_mobile()
    cid, ans = get_captcha()
    if not cid:
        return None, None, None
    sc = seed("REGISTER", mobile)
    j = api("POST", "/auth/register/check-mobile", auth=False, body={"mobile": mobile, "smsCode": sc})
    if code_of(j) != 0:
        return None, None, mobile
    rt = j["data"]["registerToken"]
    api("POST", "/auth/register/password", auth=False,
        body={"registerToken": rt, "password": "<测试账号密码>", "confirmPassword": "<测试账号密码>"})
    j = api("POST", "/auth/register/nickname", auth=False,
            body={"registerToken": rt, "nickname": nickname, "gender": "M"})
    if code_of(j) != 0:
        return None, None, mobile
    return j["data"]["token"], j["data"]["user"]["userId"], mobile


print("=" * 70)
print("补充用例（第二轮）")
print("=" * 70)

tok_a, uid_a, mob_a = register("补充用户A")
tok_b, uid_b, mob_b = register("补充用户B")
rec("登录注册", "注册两个补充用户", "code=0", f"A={bool(tok_a)} B={bool(tok_b)}", bool(tok_a) and bool(tok_b))

# 1. 图形验证码一次性
cid, ans = get_captcha()
j1 = api("POST", "/auth/sms/send", auth=False,
         body={"mobile": mob_a, "scene": "REGISTER", "captchaId": cid, "captchaCode": ans})
j2 = api("POST", "/auth/sms/send", auth=False,
         body={"mobile": mob_a, "scene": "REGISTER", "captchaId": cid, "captchaCode": ans})
rec("登录注册", "图形验证码一次性(第二次复用被拒)", "第二次 code=40003",
    f"第一次={code_of(j1)} 第二次={code_of(j2)}", code_of(j2) == 40003)

# 2. 同号重复注册 → 40901
cid, ans = get_captcha()
sc = seed("REGISTER", mob_a)
j = api("POST", "/auth/register/check-mobile", auth=False, body={"mobile": mob_a, "smsCode": sc})
rec("登录注册", "已注册手机号再次注册被拦", "code=40901", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 40901)

# 3. 错误短信验证码 → 40003，且累计错误
cid, ans = get_captcha()
sc = seed("REGISTER", rand_mobile())
j = api("POST", "/auth/register/check-mobile", auth=False, body={"mobile": "1", "smsCode": "000000"})
rec("登录注册", "非法手机号参数校验", "code=40001", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 40001)

# 4. 分页结构统一 {list, nextCursor, hasMore}
for name, path in [("任务", "/tasks"), ("论坛信息流", "/forum/feed"),
                   ("物品列表", "/trade/items"), ("评论列表-游客", "/trade/categories")]:
    j = api("GET", path, auth=False, params={"cursor": 0, "size": 5})
    d = j.get("data")
    if name == "评论列表-游客":
        continue
    keys = sorted(d.keys()) if isinstance(d, dict) else None
    rec("分页契约", f"{name}列表分页结构", "含 list/nextCursor/hasMore",
        f"keys={keys}", keys == ["hasMore", "list", "nextCursor"])

# 5. 物品列表 categoryId 过滤
cats = api("GET", "/trade/categories", auth=False).get("data") or []
if cats:
    cid0 = cats[0].get("id")
    j = api("GET", "/trade/items", auth=False, params={"categoryId": cid0, "cursor": 0, "size": 20})
    lst = (j.get("data") or {}).get("list") or []
    bad = [i.get("categoryId") for i in lst if isinstance(i, dict) and i.get("categoryId") not in (None, cid0)]
    rec("交易-首页", "物品列表按分类过滤", "列表内 categoryId 均为所查分类", f"条数={len(lst)} 异常={bad}", not bad)

# 6. TASK_CONSULT 会话
if tok_a:
    j = api("POST", "/messages/conversations", token=tok_a, body={"type": "TASK_CONSULT"})
    d = j.get("data")
    rec("站内信", "发起任务咨询会话 TASK_CONSULT", "code=0 返回 conversationId",
        f"code={code_of(j)} data={d}", code_of(j) == 0 and isinstance(d, dict) and d.get("conversationId", 0) > 0)
    # 非法会话类型
    j = api("POST", "/messages/conversations", token=tok_a, body={"type": "SPAM"})
    rec("站内信", "非法会话类型被拦", "code=40001", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 40001)

# 7. 拉黑后对方发消息被拦（双向语义）
if tok_a and tok_b:
    j = api("POST", "/messages/conversations", token=tok_a, body={"type": "TRADE", "peerUserId": uid_b})
    conv = j["data"]["conversationId"] if code_of(j) == 0 else None
    api("POST", f"/blacklist/{uid_b}", token=tok_a)
    j = api("POST", f"/messages/conversations/{conv}/messages", token=tok_b, body={"content": "被拉黑后还想说话"})
    rec("拉黑", "被拉黑方在会话内发消息被拦", "code=40301", f"code={code_of(j)} msg={j.get('message')}",
        code_of(j) == 40301, "若失败说明拉黑未作用于消息链路")
    api("DELETE", f"/blacklist/{uid_b}", token=tok_a)
    j = api("POST", f"/messages/conversations/{conv}/messages", token=tok_b, body={"content": "解除拉黑后恢复"})
    rec("拉黑", "解除拉黑后发消息恢复", "code=0", f"code={code_of(j)}", code_of(j) == 0)

# 8. 评论敏感词
if tok_a:
    j = api("POST", "/forum/posts", token=tok_a, body={"content": "正常帖子内容", "images": ["test/x.png"]})
    pid = j["data"]["id"] if code_of(j) == 0 else None
    if pid:
        j = api("POST", f"/forum/posts/{pid}/comments", token=tok_b, body={"content": "办证请找我"})
        rec("论坛-评论", "评论命中敏感词被拦", "code=40002", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 40002)
        # 超长内容
        j = api("POST", f"/forum/posts/{pid}/comments", token=tok_b, body={"content": "超" * 600})
        rec("论坛-评论", "评论超长内容被拦", "code=40001", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)
        api("DELETE", f"/forum/posts/{pid}", token=tok_a)

# 9. 物品图片+视频同时传 → 40001
if tok_a and cats:
    j = api("POST", "/trade/items", token=tok_a, body={
        "title": "图视频同传", "images": ["test/x.png"], "videoKey": "test/v.mp4",
        "description": "x", "priceFen": 100, "categoryId": cats[0]["id"], "bargainable": False})
    rec("交易-发布", "图片与视频同时传入被拦", "code=40001", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 40001)
    j = api("POST", "/trade/items", token=tok_a, body={
        "title": "无图无视频", "images": [], "videoKey": "",
        "description": "x", "priceFen": 100, "categoryId": cats[0]["id"], "bargainable": False})
    rec("交易-发布", "图片视频都不传被拦", "code=40001", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 40001)

# 10. 管理后台入口探测（黑盒边界：不应 50000 / 不应泄露信息）
j = api("POST", "/auth/login", auth=False, body={"username": "admin", "password": "wrong"}, base=ADMIN)
rec("管理后台", "管理员登录-错误凭据", "code!=0 且不是50000", f"code={code_of(j)} msg={j.get('message')}",
    code_of(j) not in (0, None, 50000))
j = api("GET", "/dashboard/summary", auth=False, base=ADMIN)
rec("管理后台", "未登录访问管理接口", "code=40101", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 40101)

# 11. 未登录上传签名
j = api("POST", "/upload/token", auth=False, body={"fileType": "IMAGE", "biz": "ITEM"})
rec("上传", "未登录请求直传签名", "code=40101", f"code={code_of(j)}", code_of(j) == 40101)

# 12. 游标越界
j = api("GET", "/trade/items", auth=False, params={"cursor": 999999999, "size": 20})
lst = (j.get("data") or {}).get("list")
rec("分页契约", "游标越界返回空列表", "code=0 且 list 为空",
    f"code={code_of(j)} 条数={len(lst) if isinstance(lst, list) else lst}",
    code_of(j) == 0 and isinstance(lst, list) and not lst)

# 13. 埋点单条事件缺 deviceId
j = api("POST", "/analytics/events", auth=False, body={"events": [{"name": "APP_LAUNCH"}]})
rec("广告埋点", "埋点缺 deviceId", "code=40001", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)

print("\n" + "=" * 70)
total = len(RESULTS)
passed = sum(1 for r in RESULTS if r["ok"] is True)
failed = sum(1 for r in RESULTS if r["ok"] is False)
print(f"补充用例合计 {total} 条：通过 {passed} / 失败 {failed}")
print("=" * 70)

with open("/tmp/shequ_test/results2.json", "w", encoding="utf-8") as f:
    json.dump(RESULTS, f, ensure_ascii=False, indent=2)
