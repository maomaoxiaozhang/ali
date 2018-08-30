package com.alibaba.ratecc.service.pipeline;

import com.alibaba.ratecc.client.api.AdvertService;
import com.alibaba.ratecc.client.constant.CommonConstant;
import com.alibaba.ratecc.client.domain.AdvertRequestDO;
import com.alibaba.ratecc.client.model.*;
import com.alibaba.ratecc.service.common.AdvertUtil;
import com.alibaba.ratecc.service.common.CommonBizLogic;
import com.taobao.feedcenter.domain.FeedItemDO;
import com.taobao.feedcenter.domain.to.FeedInfoTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <p>
 *     rp微信广告处理器，是管控处理链路中的一个环节
 *     通过{@code isAdv}判断评价内容是否包含微信广告
 *     含有微信广告的评价经过提取参数，最终调用{@code disposeAdvert}进行管控处理
 * </p>
 *
 * <p>previous step:
 * @see com.alibaba.ratecc.service.pipeline.TextWeChatRcHandler
 * </p>
 *
 * <p>next step:
 * @see com.alibaba.ratecc.service.pipeline.TextAdv4B2BHandler
 * </p>
 *
 * @author shouchang.zgq
 * @date 2018/7/26
 */

@Component
public class TextWeChatRpHandler extends Handler{
    private final AdvertService advertService;

    @Autowired
    public TextWeChatRpHandler(AdvertService advertService) {
        this.advertService = advertService;
    }

    /**
     * <p>
     *     rp微信广告处理器逻辑
     *     {@code isAdv}判断评价内容是否包含微信广告
     * </p>
     *
     * @param context   系统上下文
     */
    @Override
    protected void doHandle(Context context) {
        Object event = context.getEvent();
        if (!(event instanceof FeedInfoTO)) {
            return;
        }
        FeedInfoTO feedInfoTO = (FeedInfoTO) event;
        FeedItemDO feedItemDO = feedInfoTO.getFeedRateDO();
        if(!needDispose(feedItemDO, context)){
            return;
        }
        boolean isAdv = AdvertUtil.isAdv(feedItemDO);
        if(!isAdv) {
            return;
        }
        AdvertRequestDO advertRequestDO = new AdvertRequestDO();
        advertRequestDO.setSystem(CommonConstant.RATE_PLATFORM);
        advertRequestDO.setRateId(String.valueOf(feedItemDO.getFeedId()));
        advertRequestDO.setRaterUid(feedItemDO.getRaterUid());
        advertRequestDO.setOperator(CommonConstant.WEIXIN_MODEL);
        advertRequestDO.setSource(ControlSourceConstant.RATE);
        advertRequestDO.setReason(CommonConstant.ADV);
        //调用disposeAdvert方法对广告评价进行处理，在方法中实现了对异常的捕获，这里不需要添加
        advertService.disposeAdvert(advertRequestDO);
    }

    /**
     * <p>
     *     是否需要进行管控的判断逻辑
     * </p>
     *
     * @param feedItemDO
     *      评价订单领域对象
     *
     * @param context
     *      系统上下文
     *
     * @return  是否需要进行管控处理
     */
    private boolean needDispose(FeedItemDO feedItemDO, Context context) {
        return !CommonBizLogic.rpShouldIgnoreByType(feedItemDO, context);
    }
}
