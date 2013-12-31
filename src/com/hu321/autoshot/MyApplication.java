package com.hu321.autoshot;

import org.acra.*;
import org.acra.annotation.*;

import com.github.anrwatchdog.ANRWatchDog;

import android.app.Application;
@ReportsCrashes(formKey = "", // will not be used
formUri = "http://yqartpic.duapp.com/acra/submit",
customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.STACK_TRACE, ReportField.SHARED_PREFERENCES, ReportField.USER_APP_START_DATE, ReportField.USER_CRASH_DATE},  
mode = ReportingInteractionMode.TOAST,
resToastText = R.string.crash_toast_text)


public class MyApplication extends Application {
	public ANRWatchDog aaw = new ANRWatchDog(15000);

	@Override
	  public void onCreate() {
	    // The following line triggers the initialization of ACRA
	    super.onCreate();
	    ACRA.init(this);
	    aaw.start();

	  }
}
