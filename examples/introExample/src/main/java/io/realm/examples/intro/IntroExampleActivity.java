/*
 * Copyright 2014 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.examples.intro;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.realm.Realm;

public class IntroExampleActivity extends Activity implements IntroExamplePresenterHoster
{

    public static final String       TAG        = IntroExampleActivity.class.getName();
    private             LinearLayout rootLayout = null;

    private IntroExamplePresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_realm_basic_example);
        rootLayout = ((LinearLayout) findViewById(R.id.container));
        rootLayout.removeAllViews();

        // These operations are small enough that
        // we can generally safely run them on the UI thread.
        presenter = new IntroExamplePresenter(this);
        presenter.init();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        presenter.shutdown();
    }

    @Override
    public void showStatus(String s)
    {
        TextView tv = new TextView(this);
        tv.setText(s);
        rootLayout.addView(tv);
    }
}
