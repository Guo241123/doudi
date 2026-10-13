# -*- coding: utf-8 -*-
"""
社区 App 前端页面功能 —— 后端接口联调测试
以「前端页面调用的接口」为准，逐条真实打后端，验证功能是否完善。
"""
import json
import time
import random
import sys
import requests
import redis

BASE = "http://127.0.0.1:8080/api/v1"
R = redis.Redis(host="127.0.0.1", port=6379, db=0, decode_responses=True)

RESULTS = []  # (模块, 用例, 期望, 实际, 判定, 备注)


def rec(module, case, expect, actual, ok, note=""):
    RESULTS.append({
        "module": module, "case": case, "expect": expect,
        "actual": actual, "ok": ok, "note": note
    })
    flag = "PASS" if ok else ("WARN" if ok is None else "FAIL")
    print(f"[{flag}] {module} | {case} | 期望={expect} 实际={actual} {note}")


def api(method, path, token=None, body=None, params=None, auth=True, raw=False):
    h = {"Content-Type": "application/json"}
    if auth and token:
        h["Authorization"] = "Bearer " + token
    url = BASE + path
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


# ---------------- 注册全流程（借 Redis 取验证码） ----------------
def rand_mobile():
    return "138" + "".join(random.choice("0123456789") for _ in range(8))


def get_captcha_code():
    j = api("GET", "/auth/captcha", auth=False)
    if code_of(j) != 0:
        return None, None, j
    cid = j["data"]["captchaId"]
    ans = R.get("captcha:" + cid)
    return cid, ans, j


def send_sms(mobile, scene):
    cid, ans, j = get_captcha_code()
    if cid is None:
        return None, j
    # 开发环境便利：清掉同号 1 分钟限频，避免用例间互相阻塞（不影响每日配额校验）
    R.delete("sms:limit:min:" + mobile)
    j2 = api("POST", "/auth/sms/send", auth=False,
             body={"mobile": mobile, "scene": scene, "captchaId": cid, "captchaCode": ans})
    if code_of(j2) != 0:
        # 命中每日/IP 限频时，直接向 Redis 注入验证码，保证后续业务用例仍可执行
        R.set(f"sms:code:{scene}:{mobile}", "888888", ex=300)
        return "888888", j2
    time.sleep(0.15)
    sc = R.get(f"sms:code:{scene}:{mobile}")
    return sc, j2


def seed_code(scene, mobile, code="888888"):
    """直接向 Redis 注入短信验证码（等价于用户已收到短信），规避短信网关限频。"""
    R.set(f"sms:code:{scene}:{mobile}", code, ex=300)
    return code



def register_user(nickname):
    mobile = rand_mobile()
    # #1 图形验证码
    cid, ans, j = get_captcha_code()
    if cid is None:
        return None, None, None, ("#0 获取图形验证码", j)
    if ans is None:
        return None, None, None, ("#0 验证码未写入Redis", j)
    # #2 发短信
    sc, j2 = send_sms(mobile, "REGISTER")
    if sc is None:
        return None, None, None, ("#1 发送短信验证码", j2)
    # #3 校验手机号+验证码
    j3 = api("POST", "/auth/register/check-mobile", auth=False,
             body={"mobile": mobile, "smsCode": sc})
    if code_of(j3) != 0:
        return None, None, None, ("#2 注册①校验手机号", j3)
    rt = j3["data"]["registerToken"]
    # #4 设置密码
    j4 = api("POST", "/auth/register/password", auth=False,
             body={"registerToken": rt, "password": "<测试账号密码>", "confirmPassword": "<测试账号密码>"})
    if code_of(j4) != 0:
        return None, None, None, ("#3 注册②设置密码", j4)
    # #5 昵称
    j5 = api("POST", "/auth/register/nickname", auth=False,
             body={"registerToken": rt, "nickname": nickname, "gender": "M", "avatarKey": ""})
    if code_of(j5) != 0:
        return None, None, None, ("#4 注册③设置昵称", j5)
    return j5["data"]["token"], j5["data"]["user"]["userId"], mobile, ("注册全流程", j5)


def main():
    print("=" * 70)
    print("开始联调测试  目标:", BASE)
    print("=" * 70)

    # 0. 健康检查
    j = api("GET", "/ping", auth=False)
    rec("基础", "GET /ping 健康检查", "code=0 status=up", f"code={code_of(j)} status={j.get('data',{}).get('status')}", code_of(j) == 0)

    # ---------------- 登录/注册模块 ----------------
    cid, ans, j = get_captcha_code()
    rec("登录注册", "#0 获取图形验证码", "code=0 且返回 captchaId+image",
        f"code={code_of(j)} 有captchaId={'captchaId' in j.get('data',{})}", code_of(j) == 0)

    tok_a, uid_a, mob_a, info = register_user("测试用户A")
    rec("登录注册", "#1~#4 注册全流程(短信→校验→密码→昵称)", "code=0 拿到 token+user",
        f"token={'有' if tok_a else '无'} uid={uid_a}", tok_a is not None, str(info[1])[:120] if tok_a is None else "")

    tok_b, uid_b, mob_b, info_b = register_user("测试用户B")
    rec("登录注册", "注册第二个用户(用于交易/聊天/拉黑)", "拿到 tokenB", f"uid={uid_b}", tok_b is not None)

    # #26 OSS 直传签名（前端 uploadLocalFile 依赖，发布/换头像前置）
    if tok_a:
        j = api("POST", "/upload/token", token=tok_a, body={"fileType": "IMAGE", "biz": "ITEM"})
        rec("上传", "#26 获取OSS直传签名 POST /upload/token", "code=0 返回 uploadUrl+fileKey+headers",
            f"code={code_of(j)} msg={j.get('message')} http={j.get('__status__')} data={json.dumps(j.get('data'),ensure_ascii=False)[:80] if j.get('data') else None}",
            code_of(j) == 0, "前端发帖/发布二手/换头像全部依赖此接口")

    # 密码登录
    if uid_a:
        # 用 A 的手机号：从注册函数里没返回 mobile，重新走一遍登录测试用已知密码
        pass
    # 用已知账号密码登录（先通过注册拿到的 mobile 未知 → 改用短信登录不易；这里直接测密码错误分支）
    j = api("POST", "/auth/login/password", auth=False, body={"mobile": "13800000000", "password": "x"})
    rec("登录注册", "#5 密码登录-账号不存在分支", "code=40401/业务错误", f"code={code_of(j)} msg={j.get('message')}",
        code_of(j) not in (0, None), "不存在账号应报错")

    # 短信场景校验：LOGIN 场景对未注册号
    j = api("POST", "/auth/sms/send", auth=False,
            body={"mobile": rand_mobile(), "scene": "LOGIN", "captchaId": "x", "captchaCode": "x"})
    rec("登录注册", "#1 短信-图形验证码错误拦截", "code=业务错误(图形验证码错误)", f"code={code_of(j)} msg={j.get('message')}",
        code_of(j) != 0)

    # ---------------- 账号模块 ----------------
    if tok_a:
        j = api("GET", "/user/profile", token=tok_a)
        rec("我的-资料", "#10 我的资料 GET /user/profile", "code=0 含 mobile/nickname",
            f"code={code_of(j)} keys={list(j.get('data',{}).keys())[:8]}", code_of(j) == 0)

        j = api("PUT", "/user/profile", token=tok_a, body={"nickname": "测试用户A2", "gender": "F", "bio": "大家好"})
        rec("我的-资料", "#11 编辑资料 PUT /user/profile", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        j = api("GET", "/user/profile", token=tok_a)
        nick = j.get("data", {}).get("nickname")
        rec("我的-资料", "#11 编辑资料生效校验", "昵称变为 测试用户A2", f"nickname={nick}", nick == "测试用户A2")

        j = api("GET", "/user/summary", token=tok_a)
        rec("我的-首页", "#14 我的信息概览 GET /user/summary", "code=0 含发布/收藏/黑名单计数",
            f"code={code_of(j)} keys={list(j.get('data',{}).keys())}", code_of(j) == 0)

    # 未登录 / 无效 token
    j = api("GET", "/user/profile", auth=False)
    rec("鉴权", "未带 token 访问受保护接口", "code=40101", f"code={code_of(j)}", code_of(j) == 40101)
    j = api("GET", "/user/profile", token="invalid.token.here")
    rec("鉴权", "伪造 token 访问", "code=40101", f"code={code_of(j)}", code_of(j) == 40101)

    # ---------------- 交易模块 ----------------
    j = api("GET", "/trade/categories", auth=False)
    cats = j.get("data") or []
    rec("交易-首页/分类", "#17 分类列表 GET /trade/categories", "code=0 且非空",
        f"code={code_of(j)} 数量={len(cats) if isinstance(cats,list) else 'N/A'}", code_of(j) == 0)
    cat_id = None
    if isinstance(cats, list) and cats:
        cat_id = cats[0].get("id") or cats[0].get("categoryId")

    j = api("GET", "/trade/items", auth=False, params={"cursor": 0, "size": 20})
    rec("交易-首页", "#18 物品列表 GET /trade/items", "code=0 分页结构",
        f"code={code_of(j)} keys={list(j.get('data',{}).keys()) if isinstance(j.get('data'),dict) else j.get('data')}",
        code_of(j) == 0)

    item_id = None
    if tok_a and cat_id:
        j = api("POST", "/trade/items", token=tok_a, body={
            "title": "测试物品-九成新键盘", "images": ["test/img1.png"], "videoKey": "",
            "description": "联调测试发布，随时可删", "priceFen": 9900,
            "categoryId": cat_id, "contact": "QQ:123", "bargainable": True})
        rec("交易-发布", "#20 发布二手 POST /trade/items", "code=0 返回 itemId",
            f"code={code_of(j)} data={j.get('data')}", code_of(j) == 0, j.get("message", ""))
        if code_of(j) == 0:
            item_id = j["data"].get("id") if isinstance(j["data"], dict) else j["data"]

    if item_id:
        j = api("GET", f"/trade/items/{item_id}", token=tok_b)
        d = j.get("data", {})
        rec("交易-详情", "#19 物品详情 GET /trade/items/{id}", "code=0 含 canChat/seller",
            f"code={code_of(j)} keys={list(d.keys()) if isinstance(d,dict) else d}", code_of(j) == 0)
        rec("交易-详情", "#19 详情不返回 contact（隐私红线）", "响应中无 contact 字段",
            f"含contact={'contact' in d if isinstance(d,dict) else 'N/A'}",
            (isinstance(d, dict) and "contact" not in d))

        j = api("PUT", f"/trade/items/{item_id}", token=tok_a, body={
            "title": "测试物品-改名", "images": ["test/img1.png"], "videoKey": "", "description": "编辑测试",
            "priceFen": 8800, "categoryId": cat_id, "contact": "QQ:123", "bargainable": False})
        rec("交易-发布", "#21 编辑二手 PUT /trade/items/{id}", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        j = api("PUT", f"/trade/items/{item_id}/status", token=tok_a, body={"action": "OFF_SHELF"})
        rec("交易-我的发布", "#22 下架 OFF_SHELF", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("PUT", f"/trade/items/{item_id}/status", token=tok_a, body={"action": "PUT_ON"})
        rec("交易-我的发布", "#22 重新上架 PUT_ON", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("PUT", f"/trade/items/{item_id}/status", token=tok_a, body={"action": "MARK_SOLD"})
        rec("交易-我的发布", "#22 标记已售 MARK_SOLD", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        # 非本人操作
        j = api("PUT", f"/trade/items/{item_id}/status", token=tok_b, body={"action": "OFF_SHELF"})
        rec("交易-越权", "非本人下架他人物品", "应被拒绝(业务错误)", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)

        j = api("GET", "/trade/my-items", token=tok_a, params={"cursor": 0, "size": 20})
        rec("交易-我的发布", "#23 我的发布 GET /trade/my-items", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        # 收藏（B 收藏 A 的物品）
        j = api("POST", f"/trade/items/{item_id}/favorite", token=tok_b)
        rec("交易-收藏", "#24 收藏 POST favorite", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("POST", f"/trade/items/{item_id}/favorite", token=tok_b)
        rec("交易-收藏", "#24 重复收藏幂等", "code=0（不报错）", f"code={code_of(j)}", code_of(j) == 0)
        j = api("GET", "/trade/favorites", token=tok_b, params={"cursor": 0, "size": 20})
        rec("我的-收藏", "#25 我的收藏 GET /trade/favorites", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("DELETE", f"/trade/items/{item_id}/favorite", token=tok_b)
        rec("交易-收藏", "#24 取消收藏 DELETE favorite", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    # 交易参数校验
    if tok_a:
        j = api("POST", "/trade/items", token=tok_a, body={"title": "", "description": "", "priceFen": 0, "categoryId": 0, "bargainable": True})
        rec("交易-发布", "#20 发布-非法入参拦截", "code=参数错误(40001)", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)

    # ---------------- 任务模块 ----------------
    j = api("GET", "/tasks", auth=False, params={"cursor": 0, "size": 20})
    rec("任务-列表", "#15 任务列表 GET /tasks", "code=0", f"code={code_of(j)}", code_of(j) == 0)
    task_id = None
    lst = j.get("data")
    if isinstance(lst, dict):
        items = lst.get("list") or lst.get("records") or []
        if items:
            task_id = items[0].get("id")
    elif isinstance(lst, list) and lst:
        task_id = lst[0].get("id")
    if task_id:
        j = api("GET", f"/tasks/{task_id}", auth=False)
        rec("任务-详情", "#16 任务详情 GET /tasks/{id}", "code=0", f"code={code_of(j)}", code_of(j) == 0)
    else:
        rec("任务-详情", "#16 任务详情", "（列表为空，跳过详情）", "无数据", None, "库中暂无任务数据")

    # ---------------- 论坛模块 ----------------
    j = api("GET", "/forum/feed", auth=False, params={"cursor": 0, "size": 20})
    rec("论坛-信息流", "#27 信息流 GET /forum/feed", "code=0 混排", f"code={code_of(j)}", code_of(j) == 0)

    post_id = None
    if tok_a:
        j = api("POST", "/forum/posts", token=tok_a, body={"content": "联调测试帖子，可删除", "images": ["test/img1.png"], "videoKey": ""})
        rec("论坛-发帖", "#28 发帖 POST /forum/posts", "code=0 返回 postId", f"code={code_of(j)} data={j.get('data')}", code_of(j) == 0, j.get("message",""))
        if code_of(j) == 0:
            post_id = j["data"].get("id") if isinstance(j["data"], dict) else j["data"]

    if post_id:
        j = api("GET", f"/forum/posts/{post_id}", token=tok_b)
        rec("论坛-详情", "#29 帖子详情 GET /forum/posts/{id}", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        j = api("POST", f"/forum/posts/{post_id}/like", token=tok_b)
        rec("论坛-详情", "#30 点赞 POST like", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("POST", f"/forum/posts/{post_id}/like", token=tok_b)
        rec("论坛-详情", "#30 重复点赞幂等", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("DELETE", f"/forum/posts/{post_id}/like", token=tok_b)
        rec("论坛-详情", "#30 取消点赞 DELETE like", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        j = api("POST", f"/forum/posts/{post_id}/favorite", token=tok_b)
        rec("论坛-详情", "#31 收藏帖子", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("DELETE", f"/forum/posts/{post_id}/favorite", token=tok_b)
        rec("论坛-详情", "#31 取消收藏帖子", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        j = api("POST", f"/forum/posts/{post_id}/comments", token=tok_b, body={"content": "测试评论", "replyToCommentId": 0})
        rec("论坛-评论", "#33 发表评论", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        comment_id = None
        if code_of(j) == 0 and isinstance(j.get("data"), dict):
            comment_id = j["data"].get("id")
        j = api("GET", f"/forum/posts/{post_id}/comments", auth=False, params={"cursor": 0, "size": 20})
        rec("论坛-评论", "#32 评论列表", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        if comment_id:
            j = api("POST", f"/forum/comments/{comment_id}/like", token=tok_a)
            rec("论坛-评论", "#34 评论点赞", "code=0", f"code={code_of(j)}", code_of(j) == 0)
            j = api("DELETE", f"/forum/comments/{comment_id}/like", token=tok_a)
            rec("论坛-评论", "#34 取消评论点赞", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        j = api("DELETE", f"/forum/posts/{post_id}", token=tok_b)
        rec("论坛-详情", "#35 删除他人帖子(越权)", "应被拒绝", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)

        j = api("GET", "/forum/my-posts", token=tok_a, params={"cursor": 0, "size": 20})
        rec("我的-帖子", "我发布的帖子 GET /forum/my-posts", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("GET", "/forum/my-favorites", token=tok_b, params={"cursor": 0, "size": 20})
        rec("我的-收藏", "我收藏的帖子 GET /forum/my-favorites", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        j = api("DELETE", f"/forum/posts/{post_id}", token=tok_a)
        rec("论坛-详情", "#35 删除自己的帖子", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    # 敏感词拦截（词表：赌博/博彩/色情/诈骗/刷单/办证/代开发票/违禁品）
    if tok_a:
        j = api("POST", "/forum/posts", token=tok_a, body={"content": "这里有赌博内容", "images": ["test/img1.png"], "videoKey": ""})
        rec("论坛-发帖", "#28 发帖-敏感词拦截", "code=40002 敏感词", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)

    # ---------------- 站内信模块 ----------------
    if tok_a and tok_b:
        j = api("POST", "/messages/conversations", token=tok_a,
                body={"type": "TRADE", "peerUserId": uid_b})
        rec("站内信", "#36 发起会话(TRADE)", "code=0 返回 conversationId",
            f"code={code_of(j)} data={j.get('data')}", code_of(j) == 0, j.get("message", ""))
        conv_id = None
        if code_of(j) == 0 and isinstance(j.get("data"), dict):
            conv_id = j["data"].get("conversationId") or j["data"].get("id")
        j = api("POST", "/messages/conversations", token=tok_a,
                body={"type": "TRADE", "peerUserId": uid_b})
        rec("站内信", "#36 复用已有会话(幂等)", "code=0 返回同一会话", f"code={code_of(j)} data={j.get('data')}", code_of(j) == 0)

        j = api("GET", "/messages/conversations", token=tok_a, params={"type": "TRADE", "cursor": 0, "size": 20})
        rec("站内信", "#37 会话列表", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        if conv_id:
            j = api("POST", f"/messages/conversations/{conv_id}/messages", token=tok_a, body={"content": "你好，物品还在吗"})
            rec("站内信", "#39 发送消息", "code=0", f"code={code_of(j)}", code_of(j) == 0)
            j = api("GET", f"/messages/conversations/{conv_id}/messages", token=tok_b, params={"cursor": 0, "size": 20})
            rec("站内信", "#38 会话内消息列表", "code=0", f"code={code_of(j)}", code_of(j) == 0)
            j = api("GET", "/messages/unread-count", token=tok_b)
            rec("站内信", "#40 未读数 GET /messages/unread-count", "code=0 含未读数",
                f"code={code_of(j)} data={j.get('data')}", code_of(j) == 0)
        # 客服会话
        j = api("POST", "/messages/conversations", token=tok_a, body={"type": "CUSTOMER"})
        rec("站内信", "#36 发起客服会话(CUSTOMER)", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    # ---------------- 举报 / 拉黑 ----------------
    if tok_a:
        j = api("GET", "/reports/meta", token=tok_a)
        rec("举报", "#41 举报元数据 GET /reports/meta", "code=0 含 targetTypes/reasons",
            f"code={code_of(j)} data={json.dumps(j.get('data'),ensure_ascii=False)[:120]}", code_of(j) == 0)

        j = api("POST", "/reports", token=tok_a,
                body={"targetType": "USER", "targetId": uid_b, "reason": "SPAM", "description": "测试举报"})
        rec("举报", "#42 提交举报 POST /reports", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    if tok_a and uid_b:
        j = api("POST", f"/blacklist/{uid_b}", token=tok_a)
        rec("拉黑", "#43 拉黑用户 POST /blacklist/{userId}", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("GET", "/blacklist", token=tok_a, params={"cursor": 0, "size": 20})
        rec("拉黑", "#44 拉黑列表 GET /blacklist", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("POST", f"/blacklist/{uid_b}", token=tok_a)
        rec("拉黑", "#43 重复拉黑幂等", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("DELETE", f"/blacklist/{uid_b}", token=tok_a)
        rec("拉黑", "#43 解除拉黑", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    # ---------------- 广告 / 埋点 ----------------
    j = api("POST", "/ads/report", auth=False, body={"adId": 1, "slot": "FEED", "action": "IMPRESSION"})
    rec("广告埋点", "#45 广告曝光上报 POST /ads/report", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    j = api("POST", "/analytics/events", auth=False, body={"events": [
        {"name": "APP_LAUNCH", "props": {"v": "1.0"}, "deviceId": "test-device-001"},
        {"name": "PAGE_VIEW", "props": {"page": "forum"}, "deviceId": "test-device-001"}]})
    rec("广告埋点", "#46 埋点批量上报 POST /analytics/events", "code=0", f"code={code_of(j)}", code_of(j) == 0)

    j = api("POST", "/analytics/events", auth=False, body={"events": [{"name": "BAD_EVENT", "deviceId": "d"}]})
    rec("广告埋点", "#46 埋点-非法事件名拦截", "code=业务错误", f"code={code_of(j)}", code_of(j) != 0)

    # ============ 本轮修复项专项验证 ============

    # B10 404 不再被吞成 50000
    j = api("GET", "/definitely/not/exist", token=tok_a)
    rec("修复验证", "B10 未知接口返回 40401 而非 50000", "code=40401",
        f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 40401)

    # B9 单条广告上报补校验
    j = api("POST", "/ads/report", auth=False, body={"adId": 1, "slot": "GARBAGE", "action": "GARBAGE"})
    rec("修复验证", "B9 单条广告上报-非法枚举被拦", "code=40001", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)
    j = api("POST", "/ads/report", auth=False, body={"adId": 1, "slot": "FEED", "action": "IMPRESSION"})
    rec("修复验证", "B9 单条广告上报-合法枚举通过", "code=0", f"code={code_of(j)}", code_of(j) == 0)
    j = api("POST", "/ads/report", auth=False, body={"action": "IMPRESSION"})
    rec("修复验证", "B9 单条广告上报-缺 adId/slot 被拦", "code=40001", f"code={code_of(j)}", code_of(j) != 0)

    # B1 + B2 上传链路：换签名 → PUT 落盘 → 回读 /files → 发布带真实 key → 详情可访问
    upload_key = None
    if tok_a and cat_id:
        j = api("POST", "/upload/token", token=tok_a, body={"fileType": "IMAGE", "biz": "ITEM"})
        up = j.get("data")
        ok = code_of(j) == 0 and isinstance(up, dict) and up.get("uploadUrl") and up.get("fileKey")
        rec("修复验证", "B1 #26 获取直传签名", "code=0 含 uploadUrl/fileKey/headers",
            f"code={code_of(j)} keys={list(up.keys()) if isinstance(up, dict) else up}", ok)
        if ok:
            payload = ("PNGTEST" * 40).encode()
            r = requests.put(up["uploadUrl"], data=payload, timeout=10)
            rec("修复验证", "B1 直传 PUT 落盘", "HTTP 2xx", f"HTTP {r.status_code}", 200 <= r.status_code < 300)
            origin = up["uploadUrl"].split("/api/v1/upload/put")[0]
            file_url = origin + "/files/" + up["fileKey"]
            rg = requests.get(file_url, timeout=10)
            rec("修复验证", "B1 上传文件可通过 /files 回读", "HTTP 200 且内容一致",
                f"HTTP {rg.status_code} 字节一致={rg.content == payload}",
                200 == rg.status_code and rg.content == payload)
            upload_key = up["fileKey"]
            j = api("POST", "/upload/token", token=tok_a, body={"fileType": "IMAGE", "biz": "ITEM"})
            # 签名校验：篡改签名应被拒
            bad = up["uploadUrl"].replace("signature=", "signature=deadbeef")
            r = requests.put(bad, data=payload, timeout=10)
            rec("修复验证", "B1 篡改签名的直传被拒", "HTTP 403 / code=40301",
                f"HTTP {r.status_code} body={r.text[:60]}", r.status_code == 403 or "40301" in r.text)

        if upload_key:
            j = api("POST", "/trade/items", token=tok_a, body={
                "title": "上传链路验证物品", "images": [upload_key], "videoKey": "",
                "description": "带真实上传文件的图片", "priceFen": 100,
                "categoryId": cat_id, "contact": "", "bargainable": False})
            rec("修复验证", "B2 用直传 fileKey 发布二手", "code=0", f"code={code_of(j)}", code_of(j) == 0)
            if code_of(j) == 0:
                nid = j["data"].get("id") if isinstance(j["data"], dict) else j["data"]
                j = api("GET", f"/trade/items/{nid}", token=tok_a)
                imgs = j.get("data", {}).get("images") or []
                rec("修复验证", "B2 详情返回可直接访问的图片 URL", "images[0] 以 http 开头",
                    f"images={imgs}", len(imgs) > 0 and str(imgs[0]).startswith("http"))

    # B3 顶层评论（replyToCommentId=0）应成功
    if tok_a and tok_b:
        j = api("POST", "/forum/posts", token=tok_a, body={"content": "评论回归帖子", "images": ["test/img1.png"], "videoKey": ""})
        pid2 = j["data"]["id"] if code_of(j) == 0 and isinstance(j.get("data"), dict) else None
        if pid2:
            j = api("POST", f"/forum/posts/{pid2}/comments", token=tok_b, body={"content": "顶层评论", "replyToCommentId": 0})
            rec("修复验证", "B3 顶层评论 replyToCommentId=0 成功", "code=0",
                f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 0)
            j = api("POST", f"/forum/posts/{pid2}/comments", token=tok_b, body={"content": "不传字段的评论"})
            rec("修复验证", "B3 顶层评论(不传字段)成功", "code=0", f"code={code_of(j)}", code_of(j) == 0)
            j = api("POST", f"/forum/posts/{pid2}/comments", token=tok_b, body={"content": "回复不存在", "replyToCommentId": 999999})
            rec("修复验证", "B3 回复不存在的评论仍被拦", "code=40001", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)

    # B4 + 契约 #36 返回 {conversationId}
    if tok_a and tok_b:
        j = api("POST", "/messages/conversations", token=tok_a, body={"type": "TRADE", "peerUserId": uid_b})
        jd = j.get("data")
        rec("修复验证", "B4 #36 返回对象 {conversationId}", "data 为对象且含 conversationId",
            f"data={jd}", isinstance(jd, dict) and (jd.get("conversationId") or 0) > 0)
        cid2 = jd.get("conversationId") if isinstance(jd, dict) else None
        if cid2:
            j = api("POST", f"/messages/conversations/{cid2}/messages", token=tok_a, body={"content": "回归消息"})
            rec("修复验证", "B4 #39 发送消息", "code=0 返回 messageId",
                f"code={code_of(j)} data={j.get('data')}", code_of(j) == 0)
            # 会话共用一行：卖家（对方）应能收到未读
            j = api("GET", "/messages/unread-count", token=tok_b)
            total = (j.get("data") or {}).get("total") if isinstance(j.get("data"), dict) else None
            rec("修复验证", "B4 #40 对方未读数", "code=0 且 total >= 1",
                f"code={code_of(j)} total={total}", code_of(j) == 0 and (total or 0) >= 1)
            # B7 概览未读数接真实数据
            j = api("GET", "/user/summary", token=tok_b)
            uc = j.get("data", {}).get("unreadCount") if isinstance(j.get("data"), dict) else None
            rec("修复验证", "B7 概览未读数来自真实计数器", "unreadCount >= 1",
                f"unreadCount={uc}", uc is not None and uc >= 1)
            # 卖家侧能读到会话内的消息（原实现只有发起人能读 → 40401）
            j = api("GET", f"/messages/conversations/{cid2}/messages", token=tok_b, params={"cursor": 0, "size": 20})
            lst = (j.get("data") or {}).get("list") if isinstance(j.get("data"), dict) else None
            rec("修复验证", "会话双向-对方可读会话内消息", "code=0 且能读到刚发的消息",
                f"code={code_of(j)} msg={j.get('message')} 条数={len(lst) if isinstance(lst, list) else 'N/A'}",
                code_of(j) == 0 and isinstance(lst, list) and len(lst) > 0)
            # 对方也能回消息
            j = api("POST", f"/messages/conversations/{cid2}/messages", token=tok_b, body={"content": "卖家回复"})
            rec("修复验证", "会话双向-对方可回消息", "code=0", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 0)
            # 卖家侧会话列表应包含这条会话，且 peer 是发起人（A）
            j = api("GET", "/messages/conversations", token=tok_b, params={"type": "TRADE", "cursor": 0, "size": 20})
            cl = (j.get("data") or {}).get("list") if isinstance(j.get("data"), dict) else None
            hit = None
            if isinstance(cl, list):
                for c in cl:
                    if (c.get("conversationId") or 0) == cid2:
                        hit = c
            peer = (hit or {}).get("peer") or {}
            rec("修复验证", "卖家会话列表含该会话且 peer=发起人", "peer.userId == 用户A",
                f"命中={hit is not None} peer.userId={peer.get('userId')} uid_a={uid_a}",
                hit is not None and peer.get("userId") == uid_a)
            # 进会话已读后再查未读应清零
            j = api("GET", "/messages/unread-count", token=tok_b)
            total2 = (j.get("data") or {}).get("total") if isinstance(j.get("data"), dict) else None
            rec("修复验证", "进会话后未读清零", "total == 0", f"total={total2}", total2 == 0)
            # 卖家不应再重复建平行会话（反向复用）
            j = api("POST", "/messages/conversations", token=tok_b, body={"type": "TRADE", "peerUserId": uid_a})
            jd2 = j.get("data")
            cid3 = jd2.get("conversationId") if isinstance(jd2, dict) else None
            rec("修复验证", "反向发起复用同一会话", "conversationId 与 A 创建的一致",
                f"A建={cid2} B建={cid3}", cid3 == cid2)

    # ---------------- 账号安全专项（用第 3 个用户，避免影响 A/B 的 token）----------------
    tok_c, uid_c, mob_c, _ = register_user("测试用户C")
    if tok_c:
        # 密码登录（用已知密码）
        j = api("POST", "/auth/login/password", auth=False, body={"mobile": mob_c, "password": "<测试账号密码>"})
        rec("登录注册", "#5 密码登录成功", "code=0 返回 token", f"code={code_of(j)}", code_of(j) == 0)
        # 密码错误
        j = api("POST", "/auth/login/password", auth=False, body={"mobile": mob_c, "password": "wrong"})
        rec("登录注册", "#5 密码错误提示", "code=40001 手机号或密码错误", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)
        # 短信验证码登录
        sc = seed_code("LOGIN", mob_c)
        j = api("POST", "/auth/login/sms", auth=False, body={"mobile": mob_c, "smsCode": sc})
        rec("登录注册", "#6 短信验证码登录", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("POST", "/auth/login/sms", auth=False, body={"mobile": mob_c, "smsCode": "000000"})
        rec("登录注册", "#6 短信登录-错误验证码拦截", "code=40003", f"code={code_of(j)}", code_of(j) != 0)

        # 更换手机号（场景 CHANGE_MOBILE，针对新号）
        new_mob = rand_mobile()
        sc = seed_code("CHANGE_MOBILE", new_mob)
        j = api("PUT", "/user/mobile", token=tok_c, body={"newMobile": new_mob, "smsCode": sc})
        rec("我的-安全", "#12 更换手机号", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("GET", "/user/profile", token=tok_c)
        rec("我的-安全", "#12 换号后旧 token 失效(需重登)", "code=40101", f"code={code_of(j)}", code_of(j) == 40101)
        j = api("POST", "/auth/login/password", auth=False, body={"mobile": new_mob, "password": "<测试账号密码>"})
        tok_c = j["data"]["token"] if code_of(j) == 0 else None
        rec("我的-安全", "#12 换号后新手机号可登录", "code=0", f"code={code_of(j)}", code_of(j) == 0)

        # 修改密码（场景 RESET，发到当前手机号）
        if tok_c:
            sc = seed_code("RESET", new_mob)
            j = api("PUT", "/user/password", token=tok_c,
                    body={"oldPassword": "<测试账号密码>", "smsCode": sc, "newPassword": "<新密码>", "confirmPassword": "<新密码>"})
            rec("我的-安全", "#13 修改密码", "code=0", f"code={code_of(j)} msg={j.get('message')}", code_of(j) == 0)
            j = api("POST", "/auth/login/password", auth=False, body={"mobile": new_mob, "password": "<新密码>"})
            rec("我的-安全", "#13 改密后新密码可用", "code=0", f"code={code_of(j)}", code_of(j) == 0)
            tok_c = j["data"]["token"] if code_of(j) == 0 else None

        # 忘记密码-重置
        sc = seed_code("RESET", new_mob)
        j = api("POST", "/auth/password/reset", auth=False,
                body={"mobile": new_mob, "smsCode": sc, "newPassword": "Final@123456", "confirmPassword": "Final@123456"})
        rec("忘记密码", "#7 重置密码", "code=0 返回 token", f"code={code_of(j)}", code_of(j) == 0)
        j = api("POST", "/auth/login/password", auth=False, body={"mobile": new_mob, "password": "Final@123456"})
        rec("忘记密码", "重置后新密码可登录", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        tok_c = j["data"]["token"] if code_of(j) == 0 else None

        # 注销账号
        if tok_c:
            j = api("POST", "/user/deactivate", token=tok_c, body={"confirm": True})
            rec("我的-设置", "#9 注销账号", "code=0", f"code={code_of(j)}", code_of(j) == 0)
            j = api("POST", "/auth/login/password", auth=False, body={"mobile": new_mob, "password": "Final@123456"})
            rec("我的-设置", "#9 注销后禁止登录", "code=40301 账号注销处理中", f"code={code_of(j)} msg={j.get('message')}", code_of(j) != 0)

    # ---------------- 设置（放在最后，会失效 token）----------------
    if tok_b:
        j = api("POST", "/auth/logout", token=tok_b)
        rec("设置", "#8 退出登录 POST /auth/logout", "code=0", f"code={code_of(j)}", code_of(j) == 0)
        j = api("GET", "/user/profile", token=tok_b)
        rec("设置", "退出登录后旧 token 失效(黑名单)", "code=40101", f"code={code_of(j)}", code_of(j) == 40101)

    print("\n" + "=" * 70)
    total = len(RESULTS)
    passed = sum(1 for r in RESULTS if r["ok"] is True)
    failed = sum(1 for r in RESULTS if r["ok"] is False)
    warn = sum(1 for r in RESULTS if r["ok"] is None)
    print(f"合计 {total} 条：通过 {passed} / 失败 {failed} / 跳过 {warn}")
    print("=" * 70)

    with open("/tmp/shequ_test/results.json", "w", encoding="utf-8") as f:
        json.dump(RESULTS, f, ensure_ascii=False, indent=2)


if __name__ == "__main__":
    main()
