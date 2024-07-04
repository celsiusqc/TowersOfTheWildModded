package com.mojang.realmsclient.exception;

import com.mojang.realmsclient.client.RealmsError;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RetryCallException extends RealmsServiceException {
    public static final int DEFAULT_DELAY = 5;
    public final int delaySeconds;

    public RetryCallException(int pRetryAfter, int pHttpResultCode) {
        super(RealmsError.CustomError.retry(pHttpResultCode));
        if (pRetryAfter >= 0 && pRetryAfter <= 120) {
            this.delaySeconds = pRetryAfter;
        } else {
            this.delaySeconds = 5;
        }
    }
}
