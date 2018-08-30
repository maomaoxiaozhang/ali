package com.alibaba.ratecc.service.pipeline.advert;

import com.alibaba.rate.client.api.RateControlService;
import com.alibaba.ratecc.client.constant.CommonConstant;
import com.alibaba.ratecc.client.domain.AdvertRequestDO;
import com.alibaba.ratecc.client.model.ControlResult;
import com.alibaba.ratecc.client.model.RateControlLogDO;
import com.alibaba.ratecc.service.common.AdvertUtil;
import com.alibaba.ratecc.service.common.InternalCommonConstants;
import com.alibaba.ratecc.service.pipeline.Context;
import com.alibaba.ratecc.service.pipeline.Handler;
import com.alibaba.ratecc.service.pipeline.log.ControlLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * <p>
 *     RP广告处理器
 *     通过{@code isRp}判断是否为创新平台评价，是则调用{@code handleAdvertRate}方法进行广告处理
 * </p>
 *
 * @author shouchang.zgq
 * @date 2018/7/26
 **/
@Component
public class RpAdvertHandler extends Handler {
    private final RateControlService rpRateControlService;

    private final ControlLogService controlLogService;

    @Autowired
    public RpAdvertHandler(ControlLogService controlLogService, RateControlService rpRateControlService) {
        this.controlLogService = controlLogService;
        this.rpRateControlService = rpRateControlService;
    }

    /**
     * <p>
     *     评价过滤处理逻辑
     *     需要判断是否为rp创新平台评价订单
     *     调用{@code handleAdvertRate}进行广告的处理
     * </p>
     *
     * @param context   系统上下文
     */
    @Override
    protected void doHandle(Context context) {
        try {
            Object event = context.getEvent();
            if(!(event instanceof AdvertRequestDO)){
                return;
            }
            AdvertRequestDO advertRequestDO = (AdvertRequestDO) event;
            if (!isRp(advertRequestDO)) {
                return;
            }
            com.alibaba.rate.common.domain.control.AdvertRequestDO requestDO = new com.alibaba.rate.common.domain.control.AdvertRequestDO();
            requestDO.setRateId(Long.valueOf(advertRequestDO.getRateId()));
            requestDO.setRaterId(advertRequestDO.getRaterUid());
            requestDO.setSource(advertRequestDO.getSource());
            rpRateControlService.handleAdvertRate(requestDO);

            ControlResult controlResult = AdvertUtil.buildControlResult(advertRequestDO, context);
            RateControlLogDO rateControlLogDO = controlLogService.getControlLogDO(controlResult, context);
            context.putControlLogDO(RpAdvertHandler.class.getSimpleName(), rateControlLogDO);
        }catch (Exception e) {
            logger.warn("rp advert handler occurs an exception:", e);
            context.addException(RpAdvertHandler.class, e);
        }
    }

    /**
     * <p>
     *     判断是否为创新平台的评价订单
     * </p>
     *
     * @param advertRequestDO
     *      广告请求DO
     *
     * @return  是否为rp创新平台评价订单
     */
    private boolean isRp(AdvertRequestDO advertRequestDO) {
        return CommonConstant.RATE_PLATFORM.equals(advertRequestDO.getSystem()) ||
            InternalCommonConstants.SOURCE_RP.equals(advertRequestDO.getSystem());
    }
}
