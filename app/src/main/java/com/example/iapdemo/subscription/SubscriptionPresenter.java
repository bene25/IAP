/**
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.example.iapdemo.subscription;

import android.text.TextUtils;
import android.util.Log;

import com.example.iapdemo.callback.PurchaseIntentResultCallback;
import com.example.iapdemo.callback.QueryPurchasesCallback;
import com.example.iapdemo.callback.ProductInfoCallback;
import com.example.iapdemo.common.Constants;
import com.example.iapdemo.common.ExceptionHandle;
import com.example.iapdemo.common.IapRequestHelper;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentResult;

import java.util.List;

public class SubscriptionPresenter implements SubscriptionContract.Presenter {

    private static final String TAG = "IapPresenter";

    private SubscriptionContract.View view;

    private OwnedPurchasesResult cacheOwnedPurchasesResult;

    SubscriptionPresenter(SubscriptionContract.View view) {
        setView(view);
    }

    @Override
    public void setView(SubscriptionContract.View view) {
        if (null == view) {
            throw new NullPointerException("can not set null view");
        }
        this.view = view;
    }

    @Override
    public void load(List<String> productIds) {
        queryProducts(productIds);
        refreshSubscription();
    }

    @Override
    public void refreshSubscription() {
        querySubscriptions(new SubscriptionContract.ResultCallback<Boolean>() {
            @Override
            public void onResult(Boolean status) {
                view.updateProductStatus(cacheOwnedPurchasesResult);
            }
        });
    }

    private void queryProducts(List<String> productIds) {
        IapRequestHelper.obtainProductInfo(Iap.getIapClient(view.getActivity()), productIds, IapClient.PriceType.IN_APP_SUBSCRIPTION, new ProductInfoCallback() {
            @Override
            public void onSuccess(final ProductInfoResult result) {
                if (null == result) {
                    Log.e(TAG, "ProductInfoResult is null");
                    return;
                }

                List<ProductInfo> productInfos = result.getProductInfoList();
                view.showProducts(productInfos);
            }

            @Override
            public void onFail(Exception e) {
                int errorCode = ExceptionHandle.handle(view.getActivity(), e);
                if (ExceptionHandle.SOLVED != errorCode) {
                    Log.e(TAG, "unknown error");
                }
                view.showProducts(null);
            }
        });
    }



    private void querySubscriptions(final SubscriptionContract.ResultCallback<Boolean> callback) {
        IapRequestHelper.obtainOwnedPurchases(Iap.getIapClient(view.getActivity()), IapClient.PriceType.IN_APP_SUBSCRIPTION, new QueryPurchasesCallback() {
            @Override
            public void onSuccess(OwnedPurchasesResult result) {
                cacheOwnedPurchasesResult = result;
                callback.onResult(true);
            }

            @Override
            public void onFail(Exception e) {
                Log.e(TAG, "querySubscriptions exception", e);
                ExceptionHandle.handle(view.getActivity(), e);
                callback.onResult(false);
            }
        });
    }

    @Override
    public void buy(final String productId) {

        // clear local cache
        cacheOwnedPurchasesResult = null;
        IapClient mClient = Iap.getIapClient(view.getActivity());
        IapRequestHelper.createPurchaseIntent(mClient, productId, IapClient.PriceType.IN_APP_SUBSCRIPTION, new PurchaseIntentResultCallback() {
            @Override
            public void onSuccess(PurchaseIntentResult result) {
                if (result == null) {
                    Log.e(TAG, "PurchaseIntentResult is null");
                    return;
                }

                // you should pull up the page to complete the payment process
                IapRequestHelper.startResolutionForResult(view.getActivity(), result.getStatus(), Constants.REQ_CODE_BUY);
            }

            @Override
            public void onFail(Exception e) {
                int errorCode = ExceptionHandle.handle(view.getActivity(), e);
                if (ExceptionHandle.SOLVED != errorCode) {
                    Log.w(TAG, "createPurchaseIntent, returnCode: " + errorCode);

                    if (OrderStatusCode.ORDER_PRODUCT_OWNED == errorCode) {
                        Log.w(TAG, "already own this product");
                        showSubscription(productId);
                    } else {
                        Log.e(TAG, "unknown error");
                    }
                }
            }
        });
    }

    @Override
    public void showSubscription(String productId) {
        IapRequestHelper.showSubscription(view.getActivity(), "com.iapdemo.huawei", "101510967", productId);
    }

    @Override
    public void shouldOfferService(final String productId, final SubscriptionContract.ResultCallback<Boolean> callback) {
        if (null == callback || TextUtils.isEmpty(productId)) {
            Log.e(TAG, "ResultCallback is null or productId is empty");
            return;
        }

        if (null != cacheOwnedPurchasesResult) {
            Log.i(TAG, "using cache data");
            boolean shouldOffer = SubscriptionUtils.shouldOfferService(cacheOwnedPurchasesResult, productId);
            callback.onResult(shouldOffer);
        } else {
            querySubscriptions(new SubscriptionContract.ResultCallback<Boolean>() {
                @Override
                public void onResult(Boolean result) {
                    boolean shouldOffer = SubscriptionUtils.shouldOfferService(cacheOwnedPurchasesResult, productId);
                    callback.onResult(shouldOffer);
                }
            });
        }
    }

}
