package com.hu321.autoshot;

import org.acra.*;
import org.acra.annotation.*;

import com.github.anrwatchdog.ANRWatchDog;

import android.app.Application;
@ReportsCrashes(formKey = "", // will not be used
formUri = "http://livepic2.yiqingart.com/acra/submit",
customReportContent = { ReportField.APP_VERSION_CODE, ReportField.APP_VERSION_NAME, ReportField.ANDROID_VERSION, ReportField.STACK_TRACE, ReportField.SHARED_PREFERENCES},  
mode = ReportingInteractionMode.TOAST,
resToastText = R.string.crash_toast_text)


public class MyApplication extends Application {
	public ANRWatchDog aaw = new ANRWatchDog(10000);

	@Override
	  public void onCreate() {
	    // The following line triggers the initialization of ACRA
	    super.onCreate();
	    ACRA.init(this);
	    aaw.start();

	  }
}
