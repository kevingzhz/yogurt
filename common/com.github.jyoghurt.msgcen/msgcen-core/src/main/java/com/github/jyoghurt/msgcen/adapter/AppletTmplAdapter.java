package com.github.jyoghurt.msgcen.adapter;

import com.alibaba.fastjson.JSON;
import com.github.jyoghurt.msgcen.common.utils.MsgRegularUtil;
import com.github.jyoghurt.msgcen.common.utils.MsgTmplRuleParseUtil;
import com.github.jyoghurt.msgcen.domain.MsgTmplT;
import com.github.jyoghurt.msgcen.exception.MsgException;
import com.github.jyoghurt.msgcen.factory.MsgAdapter;
import com.github.jyoghurt.wechatbasic.common.templet.ParentTpl;
import com.github.jyoghurt.wechatbasic.common.util.AdvancedUtil;
import com.github.jyoghurt.wechatbasic.common.util.WeixinUtil;
import net.sf.json.JSONObject;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: DELL
 * Date: 2017/9/22
 * Time: 9:21
 * To change this template use File | Settings | File Templates.
 */
public class AppletTmplAdapter implements MsgAdapter, MsgTarget {
    @Override
    public void send(List<String> to, MsgTmplT msgTmpl, JSONObject param) throws MsgException {
        for (String target : to) {
            //根据模板code反射模板实体
            Class tmplClass;
            try {
                tmplClass = Class.forName("com.github.jyoghurt.msgcen.common.tmpl.wechat." + msgTmpl.getTmplCode());
                Object t = tmplClass.newInstance();
                //解析微信消息模板参数
                JSONObject tmplParam = MsgTmplRuleParseUtil.parseTmplRule(msgTmpl, param);
                //封装发送信息
                MsgRegularUtil.replaceWeChatDoubleContent(msgTmpl.getTmplContent(), t, param);
                //封装消息模板参数 若有消息模板参数则覆盖发送信息
                JSONObject sendJson = MsgRegularUtil.replaceWeChatDoubleContent(msgTmpl.getTmplContent(), t, tmplParam);
                //封装基类信息
                ParentTpl parentTpl = new ParentTpl();
                parentTpl.setTouser(target);
                parentTpl.setData(sendJson);
                parentTpl.setTemplate_id(sendJson.get("tmplId").toString());
                parentTpl.setForm_id(param.get("form_id").toString());
                //判断是否配置了模板重定向参数
                if (null != param.get("wechatRedirect")) {
                    String weChatUrl = param.get("wechatRedirect").toString();
                    //在微信模板重定向url中添加openId参数 用做跳转时的自动授权
                    if (weChatUrl.indexOf("?") > -1) {
                        weChatUrl += "&openId=" + target;
                    } else {
                        weChatUrl += "?openId=" + target;
                    }
                    parentTpl.setPage(weChatUrl);
                }
                //发送email
//                if (SwitchHandler.switchIsOpenBySwitchGroupKey("msgcen")) {
                    AdvancedUtil.sendTemple(WeixinUtil.getAccessToken().getToken(), parentTpl);
//                }
                recordMsg(target, msgTmpl, JSON.toJSONString(param), null);
            } catch (Exception e) {
                recordMsg(target, msgTmpl, JSON.toJSONString(param), e);
            }
        }
    }
}
