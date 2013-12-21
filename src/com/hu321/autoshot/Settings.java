package com.hu321.autoshot;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TimePicker;

//继承PreferenceActivity，并实现OnPreferenceChangeListener和OnPreferenceClickListener监听接口
public class Settings extends PreferenceActivity implements
		OnPreferenceClickListener, OnPreferenceChangeListener {
	// 定义相关变量
	String timeRangeBeginSetKey;
	Preference timeRangeBeginSetPref;
	String timeRangeEndSetKey;
	Preference timeRangeEndSetPref;
	String checkBoxWeekKey;
	Preference checkBoxWeekPref;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 从xml文件中添加Preference项
		addPreferencesFromResource(R.xml.preferences);

		// 获取各个Preference
//		timeRangeBeginSetKey = getResources().getString(
//				R.string.time_range_begin_set_key);
//		timeRangeBeginSetPref = findPreference(timeRangeBeginSetKey);
//		timeRangeEndSetKey = getResources().getString(
//				R.string.time_range_end_set_key);
//		timeRangeEndSetPref = findPreference(timeRangeEndSetKey);
//		checkBoxWeekKey = getResources().getString(R.string.checkbox_week_key);
//		checkBoxWeekPref = findPreference(checkBoxWeekKey);
		// //获取各个Preference
		// apiKeyKey = getResources().getString(R.string.api_key_key);
		// appsPathKey = getResources().getString(R.string.apps_path_key);
		// updateSwitchCheckPref =
		// (CheckBoxPreference)findPreference(apiKeyKey);
		// updateFrequencyListPref =
		// (ListPreference)findPreference(appsPathKey);
		// 为各个Preference注册监听接口
//		timeRangeBeginSetPref.setOnPreferenceClickListener(this);
//		timeRangeEndSetPref.setOnPreferenceClickListener(this);
//		checkBoxWeekPref.setOnPreferenceClickListener(this);
		// updateSwitchCheckPref.setOnPreferenceClickListener(this);
		// updateFrequencyListPref.setOnPreferenceClickListener(this);
		// updateSwitchCheckPref.setOnPreferenceChangeListener(this);
		// updateFrequencyListPref.setOnPreferenceChangeListener(this);	
		
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		// TODO Auto-generated method stub

		Log.v("SystemSetting", "preference is clicked");
		Log.v("Key_SystemSetting", preference.getKey());
		// 判断是哪个Preference被点击了
//		if (preference.getKey().equals(timeRangeBeginSetKey)) {
//			Log.v("SystemSetting", "timeRangeBeginSetKey preference is clicked");
//			showTimeRangeSelectDiaglog(true);
//		} else if (preference.getKey().equals(timeRangeEndSetKey)) {
//			Log.v("SystemSetting", "timeRangeEndSetKey preference is clicked");
//			showTimeRangeSelectDiaglog(false);
//		} else if (preference.getKey().equals(checkBoxWeekKey)) {
//			Log.v("SystemSetting", "checkBoxWeekKey preference is clicked");
//			showWeekSelectDiaglog();
//		} else {
//			return false;
//		}
		return true;
	}

	private void showTimeRangeSelectDiaglog(Boolean isBegin) {
		Integer hour;
		Integer minute;

		// 取得属于整个应用程序的SharedPreferences
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);
		if (isBegin) {
			// 获取设置界面PreferenceActivity中各个Preference的值
			String beginHourKey = getResources().getString(
					R.string.time_range_begin_hour_key);
			String beginMinuteKey = getResources().getString(
					R.string.time_range_begin_minute_key);
			hour = settings.getInt(beginHourKey, 7);
			minute = settings.getInt(beginMinuteKey, 30);
		} else {
			// 获取设置界面PreferenceActivity中各个Preference的值
			String endHourKey = getResources().getString(
					R.string.time_range_end_hour_key);
			String endMinuteKey = getResources().getString(
					R.string.time_range_end_minute_key);
			hour = settings.getInt(endHourKey, 17);
			minute = settings.getInt(endMinuteKey, 30);
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View view = View.inflate(this, R.layout.time_range_picker, null);
		final TimePicker timePicker = (android.widget.TimePicker) view
				.findViewById(R.id.time_picker);
		builder.setView(view);

		timePicker.setIs24HourView(true);
		timePicker.setCurrentHour(hour);
		timePicker.setCurrentMinute(minute);

		if (isBegin) {
			builder.setTitle(getResources().getString(
					R.string.time_range_begin_set_tittle));
			builder.setPositiveButton("确  定",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							SharedPreferences.Editor editor = settings.edit();

							String beginHourKey = getResources().getString(
									R.string.time_range_begin_hour_key);
							String beginMinuteKey = getResources().getString(
									R.string.time_range_begin_minute_key);

							editor.putInt(beginHourKey,
									timePicker.getCurrentHour());
							editor.putInt(beginMinuteKey,
									timePicker.getCurrentMinute());

							editor.commit();
							dialog.cancel();
						}
					});
		} else {
			builder.setTitle(getResources().getString(
					R.string.time_range_end_set_tittle));
			builder.setPositiveButton("确  定",
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {

							SharedPreferences.Editor editor = settings.edit();

							String endHour = getResources().getString(
									R.string.time_range_end_hour_key);
							String endMinute = getResources().getString(
									R.string.time_range_end_minute_key);

							editor.putInt(endHour, timePicker.getCurrentHour());
							editor.putInt(endMinute,
									timePicker.getCurrentMinute());

							editor.commit();
							dialog.cancel();
						}
					});
		}

		Dialog dialog = builder.create();
		dialog.show();
	}

	private void showWeekSelectDiaglog() {
		// 取得属于整个应用程序的SharedPreferences
		final SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(this);

		// 获取设置界面PreferenceActivity中各个Preference的值
		final String weekKey = getResources().getString(
				R.string.checkbox_week_key);

		String weeks = settings.getString(weekKey,
				getResources().getString(R.string.checkbox_week_default_value));

		String[] week = weeks.split(",");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		View view = View.inflate(this, R.layout.week_checkbox, null);
		final CheckBox week1 = (android.widget.CheckBox) view
				.findViewById(R.id.checkBox_week_1);
		final CheckBox week2 = (android.widget.CheckBox) view
				.findViewById(R.id.checkBox_week_2);
		final CheckBox week3 = (android.widget.CheckBox) view
				.findViewById(R.id.checkBox_week_3);
		final CheckBox week4 = (android.widget.CheckBox) view
				.findViewById(R.id.checkBox_week_4);
		final CheckBox week5 = (android.widget.CheckBox) view
				.findViewById(R.id.checkBox_week_5);
		final CheckBox week6 = (android.widget.CheckBox) view
				.findViewById(R.id.checkBox_week_6);
		final CheckBox week7 = (android.widget.CheckBox) view
				.findViewById(R.id.checkBox_week_7);

		builder.setView(view);

		week1.setChecked(Boolean.valueOf(week[0]));
		week2.setChecked(Boolean.valueOf(week[1]));
		week3.setChecked(Boolean.valueOf(week[2]));
		week4.setChecked(Boolean.valueOf(week[3]));
		week5.setChecked(Boolean.valueOf(week[4]));
		week6.setChecked(Boolean.valueOf(week[5]));
		week7.setChecked(Boolean.valueOf(week[6]));

		builder.setTitle(getResources()
				.getString(R.string.checkbox_week_tittle));
		builder.setPositiveButton("确  定",
				new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {

						SharedPreferences.Editor editor = settings.edit();

						String value = String.valueOf(week1.isChecked()) + ","
								+ String.valueOf(week2.isChecked()) + ","
								+ String.valueOf(week3.isChecked()) + ","
								+ String.valueOf(week4.isChecked()) + ","
								+ String.valueOf(week5.isChecked()) + ","
								+ String.valueOf(week6.isChecked()) + ","
								+ String.valueOf(week7.isChecked());

						editor.putString(weekKey, value);

						editor.commit();
						dialog.cancel();
					}
				});

		Dialog dialog = builder.create();
		dialog.show();
	}

}
