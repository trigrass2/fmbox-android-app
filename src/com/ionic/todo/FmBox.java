/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.ionic.todo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import org.apache.cordova.*;
import org.ionic.bluetooth.BlueTooth;

public class FmBox extends CordovaActivity {
	
	private void restartApp() {
		Intent i = getBaseContext().getPackageManager()  
		        .getLaunchIntentForPackage(getBaseContext().getPackageName());  
		i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);  
		startActivity(i);  		
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.init();
		
		Log.d("FmBox", "create");
		BlueTooth.init(this);
		super.loadUrl(Config.getStartUrl());
	}
	
}
