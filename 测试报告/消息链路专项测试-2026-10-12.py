# -*- coding: utf-8 -*-
"""
站内信 / 消息链路 专项联调测试（2026-10-12 v2）
聚焦：联系卖家(TRADE) / 客服(CUSTOMER) / 任务咨询(TASK_CONSULT) + 后台工作台(#55)
本轮新增：管理员回复闭环验证（用户→客服→管理员回复→用户可见）
"""
import json
import time
import random
import requests
import redis

BASE = "http://127.0.0.1:8080/api/v1"
ADMIN_BASE = "http://127.0.0.1:8080/api/admin/v1"
R = redis.Redis(host="127.0.0.1", port=6379, db=0, decode_responses=True)

RESULTS = []


def rec(module, case, expect, actual, ok, note=""):
    RESULTS.append({"module": module, "case": case, "expect": expect,
                    "actual": actual, "ok": ok, "note": note})
    flag = "PASS" if ok else ("WARN" if ok is None else "FAIL")
    print(f"[{flag}] {module} | {case} | 期望={expect} 实际={actual} {note}")


def api(method, path, token=None, body=None, params=None, auth=True, admin=False):
    h = {"Content-Type": "application/json"}
    if auth and token:
        h["Authorization"] = "Bearer " + token
    url = (ADMIN_BASE + path) if admin else (BASE + path)
    try:
        resp = requests.request(method, url, headers=h, json=body, params=params, timeout=10)
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
    return "138" + "".join(random.choice("0123456789") for _ in range(8))


def get_captcha_code():
    j = api("GET", "/auth/captcha", auth=False)
    if code_of(j) != 0:
        return None, None
    return j["data"]["captchaId"], R.get("captcha:" + j["data"]["captchaId"])


def send_sms_code(mobile, scene):
    cid, ans = get_captcha_code()
    if cid is None:
        return None
    R.delete("sms:limit:min:" + mobile)
    j2 = api("POST", "/auth/sms/send", auth=False,
             body={"mobile": mobile, "scene": scene, "captchaId": cid, "captchaCode": ans})
    if code_of(j2) != 0:
        R.set(f"sms:code:{scene}:{mobile}", "888888", ex=300)
        return "888888"
    time.sleep(0.15)
    return R.get(f"sms:code:{scene}:{mobile}")


def register_user(nickname):
    mobile = rand_mobile()
    sc = send_sms_code(mobile, "REGISTER")
    if sc is None:
        return None, None
    j3 = api("POST", "/auth/register/check-mobile", auth=False, body={"mobile": mobile, "smsCode": sc})
    if code_of(j3) != 0:
        return None, None
    rt = j3["data"]["registerToken"]
    j4 = api("POST", "/auth/register/password", auth=False,
             body={"registerToken": rt, "password": "<测试账号密码>", "confirmPassword": "<测试账号密码>"})
    if code_of(j4) != 0:
        return None, None
    j5 = api("POST", "/auth/register/nickname", auth=False,
             body={"registerToken": rt, "nickname": nickname, "gender": "M", "avatarKey": ""})
    if code_of(j5) != 0:
        return None, None
    return j5["data"]["token"], j5["data"]["user"]["userId"]


def main():
    print("=" * 76)
    print("站内信链路专项测试 v2（含后台工作台 #55）  目标:", BASE)
    print("=" * 76)

    j = api("GET", "/ping", auth=False)
    rec("基础", "后端健康检查", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    tok_b, uid_b = register_user("链路买方")
    tok_s, uid_s = register_user("链路卖方")
    rec("准备", "注册买方/卖方", "均拿到 token",
        f"B uid={uid_b} S uid={uid_s}", tok_b is not None and tok_s is not None)
    if not tok_b or not tok_s:
        print("!! 注册失败，终止")
        return

    print("\n---------------- 一、联系卖家 TRADE ----------------")
    j = api("POST", "/messages/conversations", token=tok_b, body={"type": "TRADE", "peerUserId": uid_s})
    cid = (j.get("data") or {}).get("conversationId") if code_of(j) == 0 else None
    rec("联系卖家", "#36 买方发起 TRADE 会话", "code=0 返回 {conversationId}",
        f"code={code_of(j)} data={j.get('data')}", code_of(j) == 0 and isinstance(cid, (int, float)))

    j = api("GET", f"/messages/conversations/{cid}/messages", token=tok_b)
    lst = (j.get("data") or {}).get("list")
    rec("联系卖家", "#38 新会话消息列表为空", "code=0 且 list=[]",
        f"code={code_of(j)} list={lst}", code_of(j) == 0 and lst == [])

    j = api("POST", f"/messages/conversations/{cid}/messages", token=tok_b, body={"content": "你好，物品还在吗？"})
    rec("联系卖家", "#39 买方发送消息", "code=0 返回 messageId",
        f"code={code_of(j)} data={j.get('data')}", code_of(j) == 0 and (j.get("data") or {}).get("messageId"))

    j = api("GET", "/messages/unread-count", token=tok_b)
    d = j.get("data") or {}
    rec("联系卖家", "#40 买方未读（自己发的不计）", "total=0",
        f"code={code_of(j)} data={d}", code_of(j) == 0 and d.get("total") == 0)

    j = api("GET", "/messages/conversations", token=tok_s, params={"type": "TRADE", "cursor": 0, "size": 20})
    arr = (j.get("data") or {}).get("list") or []
    mine = [c for c in arr if c.get("conversationId") == cid]
    peer_nick = (mine[0].get("peer") or {}).get("nickname") if mine else None
    rec("联系卖家", "#37 卖方能看到会话且 peer=买方昵称", "命中 cid 且 peer.nickname=链路买方",
        f"code={code_of(j)} 命中={bool(mine)} peer={peer_nick}", bool(mine) and peer_nick == "链路买方")

    j = api("GET", "/messages/unread-count", token=tok_s)
    d = j.get("data") or {}
    rec("联系卖家", "#40 卖方未读 +1", "total=1 且 byType.TRADE=1",
        f"code={code_of(j)} data={d}", code_of(j) == 0 and d.get("total") == 1 and (d.get("byType") or {}).get("TRADE") == 1)

    j = api("GET", f"/messages/conversations/{cid}/messages", token=tok_s)
    lst = (j.get("data") or {}).get("list") or []
    first_ok = len(lst) == 1 and lst[0].get("senderId") == uid_b and lst[0].get("content") == "你好，物品还在吗？"
    rec("联系卖家", "#38 卖方读到买方的消息", "1 条 senderId=买方 content 一致",
        f"code={code_of(j)} 条数={len(lst)}", code_of(j) == 0 and first_ok)

    j = api("GET", "/messages/unread-count", token=tok_s)
    d = j.get("data") or {}
    rec("联系卖家", "#40 卖方进会话后未读清零", "total=0",
        f"code={code_of(j)} data={d}", code_of(j) == 0 and d.get("total") == 0)

    j = api("POST", f"/messages/conversations/{cid}/messages", token=tok_s, body={"content": "还在的，随时可看"})
    rec("联系卖家", "#39 卖方回复", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    # 先查未读，再读消息（读消息会清零未读）——顺序很关键
    j = api("GET", "/messages/unread-count", token=tok_b)
    d = j.get("data") or {}
    rec("联系卖家", "#40 买方未读 +1（读之前查）", "total=1",
        f"code={code_of(j)} data={d}", code_of(j) == 0 and d.get("total") == 1)

    j = api("GET", f"/messages/conversations/{cid}/messages", token=tok_b, params={"cursor": 0, "size": 20})
    lst = (j.get("data") or {}).get("list") or []
    got = any(m.get("senderId") == uid_s and m.get("content") == "还在的，随时可看" for m in lst)
    rec("联系卖家", "#38 买方能看到卖方回复（双向）", "2 条且含卖方回复",
        f"code={code_of(j)} 条数={len(lst)}", code_of(j) == 0 and len(lst) == 2 and got)

    j = api("POST", "/messages/conversations", token=tok_s, body={"type": "TRADE", "peerUserId": uid_b})
    cid_rev = (j.get("data") or {}).get("conversationId") if code_of(j) == 0 else None
    rec("联系卖家", "#36 卖方反向发起复用同一会话", f"返回 {cid}",
        f"code={code_of(j)} conversationId={cid_rev}", cid_rev == cid)

    tok_x, uid_x = register_user("链路路人")
    j = api("GET", f"/messages/conversations/{cid}/messages", token=tok_x)
    rec("联系卖家", "越权：非会话成员读消息被拒", "非 0（40401）",
        f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)

    print("\n---------------- 二、客服 CUSTOMER + 后台回复闭环 ----------------")
    j = api("POST", "/messages/conversations", token=tok_b, body={"type": "CUSTOMER"})
    ccid = (j.get("data") or {}).get("conversationId") if code_of(j) == 0 else None
    rec("客服", "#36 发起 CUSTOMER 会话", "code=0 返回 conversationId",
        f"code={code_of(j)} data={j.get('data')}", code_of(j) == 0 and isinstance(ccid, (int, float)))

    j = api("POST", f"/messages/conversations/{ccid}/messages", token=tok_b, body={"content": "客服你好，物品下架了怎么恢复？"})
    rec("客服", "#39 用户发送消息", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    j = api("GET", "/messages/conversations", token=tok_b, params={"type": "CUSTOMER", "cursor": 0, "size": 20})
    arr = (j.get("data") or {}).get("list") or []
    peer_nick = (arr[0].get("peer") or {}).get("nickname") if arr else None
    rec("客服", "#37 会话列表 peer=管理员昵称", "peer.nickname=社区管理员",
        f"code={code_of(j)} peer={peer_nick}", bool(arr) and peer_nick == "社区管理员")

    # --- 后台工作台 ---
    ja = api("POST", "/auth/login", admin=True, auth=False, body={"username": "admin", "password": "<管理员密码>"})
    atok = (ja.get("data") or {}).get("token") if code_of(ja) == 0 else None
    rec("后台工作台", "管理员登录", "code=0 拿到 admin token",
        f"code={code_of(ja)}", code_of(ja) == 0 and atok is not None)

    if atok:
        j1 = api("GET", "/conversations", token=atok, admin=True, params={"page": 1, "size": 20, "type": "CUSTOMER"})
        d = j1.get("data") or {}
        arr = d.get("list") or []
        hit = [c for c in arr if c.get("conversationId") == ccid]
        u = (hit[0].get("user") or {}) if hit else {}
        rec("后台工作台", "#55 会话列表（分页+user+未读）", "命中 ccid 且 user.nickname=链路买方 且未读=1",
            f"code={code_of(j1)} total={d.get('total')} 命中={bool(hit)} user={u.get('nickname')} unread={hit[0].get('unreadCount') if hit else None}",
            code_of(j1) == 0 and bool(hit) and u.get("nickname") == "链路买方" and hit[0].get("unreadCount") == 1)

        j2 = api("GET", f"/conversations/{ccid}/messages", token=atok, admin=True)
        lst2 = (j2.get("data") or {}).get("list") or []
        rec("后台工作台", "#55 管理员查看会话消息", "1 条且内容一致",
            f"code={code_of(j2)} 条数={len(lst2)}", code_of(j2) == 0 and len(lst2) == 1
            and lst2[0].get("content") == "客服你好，物品下架了怎么恢复？")

        j3 = api("POST", f"/conversations/{ccid}/messages", token=atok, admin=True, body={"content": "您好，重新上架需等待审核，一般 24 小时内完成。"})
        rec("后台工作台", "#55 管理员回复", "code=0 返回 messageId",
            f"code={code_of(j3)} data={j3.get('data')}", code_of(j3) == 0 and (j3.get("data") or {}).get("messageId"))

        # 用户侧看到回复 + 未读 +1
        j4 = api("GET", "/messages/unread-count", token=tok_b)
        d4 = j4.get("data") or {}
        rec("后台工作台", "用户侧未读 +1（客服回复后）", "total=1 且 byType.CUSTOMER=1",
            f"code={code_of(j4)} data={d4}", code_of(j4) == 0 and d4.get("total") == 1
            and (d4.get("byType") or {}).get("CUSTOMER") == 1)

        j5 = api("GET", f"/messages/conversations/{ccid}/messages", token=tok_b)
        lst5 = (j5.get("data") or {}).get("list") or []
        has_reply = any(m.get("content") == "您好，重新上架需等待审核，一般 24 小时内完成。" for m in lst5)
        rec("后台工作台", "客服闭环：用户能看到管理员回复", "2 条且含管理员回复",
            f"code={code_of(j5)} 条数={len(lst5)}", code_of(j5) == 0 and len(lst5) == 2 and has_reply)

        # 再查一次未读：进会话后应清零
        j6 = api("GET", "/messages/unread-count", token=tok_b)
        d6 = j6.get("data") or {}
        rec("后台工作台", "用户进会话后未读清零", "total=0",
            f"code={code_of(j6)} data={d6}", code_of(j6) == 0 and d6.get("total") == 0)

        # TASK_CONSULT 优先排序
        j7 = api("GET", "/conversations", token=atok, admin=True, params={"page": 1, "size": 20})
        arr7 = (j7.get("data") or {}).get("list") or []
        first_type = arr7[0].get("type") if arr7 else None
        any_task = any(c.get("type") == "TASK_CONSULT" for c in arr7)
        rec("后台工作台", "#55 任务咨询优先排序", "首条为 TASK_CONSULT（若存在任务会话）",
            f"首条 type={first_type} 含任务会话={any_task}", (not any_task) or first_type == "TASK_CONSULT")

        # 权限：App 用户 token 不能调后台
        j8 = api("GET", "/conversations", token=tok_b, admin=True)
        rec("后台工作台", "越权：App token 调后台被拒", "40101/40301",
            f"code={code_of(j8)} msg={j8.get('message')}", code_of(j8) in (40101, 40301))

    print("\n---------------- 三、任务咨询 TASK_CONSULT ----------------")
    j = api("POST", "/messages/conversations", token=tok_b, body={"type": "TASK_CONSULT", "taskId": 3})
    tcid = (j.get("data") or {}).get("conversationId") if code_of(j) == 0 else None
    rec("任务咨询", "#36 发起任务会话（taskId=3）", "code=0",
        f"code={code_of(j)} data={j.get('data')} msg={j.get('message')}",
        code_of(j) == 0 and isinstance(tcid, (int, float)))

    if tcid:
        j = api("GET", "/messages/conversations", token=tok_b, params={"type": "TASK_CONSULT", "cursor": 0, "size": 20})
        arr = (j.get("data") or {}).get("list") or []
        tr = (arr[0].get("taskRef") or {}) if arr else {}
        rec("任务咨询", "#37 会话带 taskRef{taskId,title}", "taskRef 有 taskId+title",
            f"code={code_of(j)} taskRef={tr}", bool(arr) and tr.get("taskId") == 3 and bool(tr.get("title")))
        j = api("POST", f"/messages/conversations/{tcid}/messages", token=tok_b, body={"content": "这个任务还招人吗"})
        rec("任务咨询", "#39 发送消息", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("POST", "/messages/conversations", token=tok_b, body={"type": "TASK_CONSULT", "taskId": 999999})
        rec("任务咨询", "#36 任务不存在应被拒", "非 0（40401）",
            f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)

    print("\n---------------- 四、拉黑拦截 ----------------")
    j = api("POST", f"/blacklist/{uid_s}", token=tok_b)
    rec("拉黑", "#43 买方拉黑卖方", "code=0", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 0)
    j = api("POST", f"/messages/conversations/{cid}/messages", token=tok_b, body={"content": "拉黑后还能发吗"})
    rec("拉黑", "#39 拉黑后发送被拒", "40301",
        f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 40301)
    j = api("DELETE", f"/blacklist/{uid_s}", token=tok_b)
    rec("拉黑", "#43 解除拉黑", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    print("\n" + "=" * 76)
    total = len(RESULTS)
    passed = sum(1 for r in RESULTS if r["ok"])
    failed = [r for r in RESULTS if not r["ok"]]
    print(f"合计 {total} 条：PASS {passed} / FAIL {len(failed)}")
    for r in failed:
        print(f"  !! {r['module']} | {r['case']} | 实际={r['actual']}")
    print("=" * 76)

    with open("消息链路专项结果-2026-10-12.json", "w", encoding="utf-8") as f:
        json.dump({"total": total, "passed": passed, "failed": len(failed), "results": RESULTS},
                  f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
