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

package com.example.iapdemo.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.example.iapdemo.adapter.BillListAdapter;
import com.example.iapdemo.callback.QueryPurchasesCallback;
import com.example.iapdemo.common.CipherUtil;
import com.example.iapdemo.common.ExceptionHandle;
import com.example.iapdemo.common.IapRequestHelper;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.iapdemo.huawei.R;

import java.util.ArrayList;
import java.util.List;

public class PurchaseHistoryActivity extends AppCompatActivity {

    private String TAG = "PurchaseHistoryActivity";

    private ListView billListView;

    List<String> billList = new ArrayList<String>();

    private static String continuationToken = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_history);
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        findViewById(R.id.bill_listview).setVisibility(View.GONE);
        billListView = findViewById(R.id.bill_listview);

    }

    @Override
    protected void onResume() {
        super.onResume();
        queryHistoryInterface();
    }

    private void queryHistoryInterface() {
        IapClient iapClient = Iap.getIapClient(this);
        IapRequestHelper.obtainOwnedPurchaseRecord(iapClient, IapClient.PriceType.IN_APP_CONSUMABLE, continuationToken, new QueryPurchasesCallback() {
            @Override
            public void onSuccess(OwnedPurchasesResult result) {
                Log.i(TAG, "obtainOwnedPurchaseRecord, success");
                List<String> inAppPurchaseDataList = result.getInAppPurchaseDataList();
                List<String> signatureList = result.getInAppSignature();
                if (inAppPurchaseDataList == null) {
                    onFinish();
                    return;
                }
                Log.i(TAG, "list size: " + inAppPurchaseDataList.size());
                for (int i = 0; i < signatureList.size(); i++) {
                    boolean success = CipherUtil.doCheck(inAppPurchaseDataList.get(i), signatureList.get(i), CipherUtil.getPublicKey());
                    if (success) {
                        billList.add(inAppPurchaseDataList.get(i));
                    }
                }
                continuationToken = result.getContinuationToken();
                if (!TextUtils.isEmpty(continuationToken)) {
                    queryHistoryInterface();
                } else {
                    onFinish();
                }

            }

            @Override
            public void onFail(Exception e) {
                Log.e(TAG, "obtainOwnedPurchaseRecord, " + e.getMessage());
                int returnCode = ExceptionHandle.handle(PurchaseHistoryActivity.this, e);
                onFinish();
            }
        });
    }

    private void onFinish() {
        findViewById(R.id.progressBar).setVisibility(View.GONE);
        findViewById(R.id.bill_listview).setVisibility(View.VISIBLE);
        Log.i(TAG, "onFinish");
        BillListAdapter billAdapter = new BillListAdapter(PurchaseHistoryActivity.this, billList);
        billListView.setAdapter(billAdapter);
    }

}
