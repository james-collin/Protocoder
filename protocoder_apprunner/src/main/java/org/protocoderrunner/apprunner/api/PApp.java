/*
 * Protocoder 
 * A prototyping platform for Android devices 
 * 
 * Victor Diaz Barrales victormdb@gmail.com
 *
 * Copyright (C) 2014 Victor Diaz
 * Copyright (C) 2013 Motorola Mobility LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions: 
 * 
 * The above copyright notice and this permission notice shall be included in all 
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN 
 * THE SOFTWARE.
 * 
 */

package org.protocoderrunner.apprunner.api;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.support.v4.app.NotificationCompat;

import org.mozilla.javascript.NativeObject;
import org.protocoderrunner.R;
import org.protocoderrunner.apidoc.annotation.ProtoMethod;
import org.protocoderrunner.apidoc.annotation.ProtoMethodParam;
import org.protocoderrunner.apprunner.AppRunnerActivity;
import org.protocoderrunner.apprunner.PInterface;
import org.protocoderrunner.apprunner.api.other.PEvents;
import org.protocoderrunner.apprunner.api.other.PLiveCodingFeedback;
import org.protocoderrunner.project.Project;
import org.protocoderrunner.project.ProjectManager;
import org.protocoderrunner.project.SchedulerManager;
import org.protocoderrunner.utils.ExecuteCmd;
import org.protocoderrunner.utils.FileIO;

public class PApp extends PInterface {

    public interface onAppStatus {

        public void onStart();

        public void onPause();

        public void onResume();

        public void onStop();
    }

    PEvents pevents;

    public PApp(Context a) {
        super(a);
        pevents = new PEvents(a);
    }


    //TODO reenable this
    //
    //@APIMethod(description = "get the script runner context", example = "")
    //public AppRunnerFragment getContext() {
    //	return (AppRunnerFragment) mContext;
    //}

    //TODO
    @ProtoMethod(description = "", example = "")
    public void delayedAlarm(int delay, boolean alarmRepeat, boolean wakeUpScreen) {
        Project p = ProjectManager.getInstance().getCurrentProject();
        SchedulerManager.getInstance(getContext()).setAlarmDelayed(p, delay, alarmRepeat, wakeUpScreen);
    }

    //TODO
    @ProtoMethod(description = "", example = "")
    public void delayedAlarm(int hour, int minute, int second, boolean wakeUpScreen) {
        Project p = ProjectManager.getInstance().getCurrentProject();
        SchedulerManager.getInstance(getContext()).setAlarm(p, hour, minute, second, wakeUpScreen);
    }

    //TODO
    @ProtoMethod(description = "", example = "")
    public void exactAlarm(int hour, int minute, int second, boolean wakeUpScreen) {
        Project p = ProjectManager.getInstance().getCurrentProject();
        SchedulerManager.getInstance(getContext()).setAlarm(p, hour, minute, second, wakeUpScreen);
    }


    @ProtoMethod(description = "", example = "")
    @ProtoMethodParam(params = {"type", "data"})
    public void getSharedData(String type, String data) {

    }


    @ProtoMethod(description = "close the running script", example = "")
    public void close() {
        getActivity().finish();
    }

    @android.webkit.JavascriptInterface

    @ProtoMethod(description = "evaluate mContext script", example = "")
    @ProtoMethodParam(params = {"code"})
    public void eval(String code) {
        getActivity().mAppRunnerFragment.interp.eval(code);
    }


    @ProtoMethod(description = "loads and external file containing code", example = "")
    @ProtoMethodParam(params = {"fileName"})
    public void load(String filename) {
        String code = FileIO.loadFile(filename);
        getActivity().mAppRunnerFragment.interp.eval(code);
    }


    @ProtoMethod(description = "loads mContext library stored in the <i>libraries</i>' folder", example = "")
    @ProtoMethodParam(params = {"libraryName"})
    public void loadLibrary(String name) {
        String code = FileIO.loadFile("../../libraries/" + name + "/main.js");
        getActivity().mAppRunnerFragment.interp.eval(code);
    }

    //TODO way to cancel notification and come back to the script
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)

    @ProtoMethod(description = "", example = "")
    @ProtoMethodParam(params = {"id", "title", "description"})
    public void notification(int id, String title, String description) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getContext())
                .setSmallIcon(R.drawable.app_icon).setContentTitle(title).setContentText(description);
        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(getContext(), AppRunnerActivity.class);

        // The stack builder object will contain an artificial back stack for
        // the started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(getContext());
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(AppRunnerActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) getContext().getSystemService(
                getContext().NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(id, mBuilder.build());

    }

    // TOFIX not working yet
    //
    // @APIMethod(description = "", example = "")
    // @APIParam(params = { "id" })
    public void shareImage(String imagePath) {
        ContentValues values = new ContentValues();
        values.put(MediaColumns.MIME_TYPE, "image/png");
        //values.put(MediaColumns.DATA, AppRunnerSettings.get().project.getStoragePath() + "/" + imagePath);
        Uri uri = getContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/png");

        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        getContext().startActivity(shareIntent);
    }


    @ProtoMethod(description = "launch the share intent with the included text", example = "")
    @ProtoMethodParam(params = {"text"})
    public void shareText(String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/*");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        getContext().startActivity(shareIntent);
    }


    @ProtoMethod(description = "get the current project HTTP URL", example = "")
    public String projectUrl() {
        String url = ProjectManager.getInstance().getCurrentProject().getServingURL();
        return url;
    }


    @ProtoMethod(description = "get the current project path", example = "")
    public String projectPath() {
        String url = ProjectManager.getInstance().getCurrentProject().getStoragePath() + "/";
        return url;
    }

    // --------- doNotExecute ---------//
    public interface DoNothingCB {
        void event();
    }


    @ProtoMethod(description = "this dummy function doesn't execute the callback", example = "")
    @ProtoMethodParam(params = {"function()"})
    public void doNotExecute(DoNothingCB callbackfn) {

    }


    @ProtoMethod(description = "execute a shell command", example = "")
    @ProtoMethodParam(params = {"cmd", "function(data)"})
    public ExecuteCmd executeCommand(final String cmd, final ExecuteCmd.ExecuteCommandCB callbackfn) {

        return new ExecuteCmd(cmd, callbackfn);
    }


    @ProtoMethod(description = "shows mContext feedback overlay with the live-executed code", example = "")
    @ProtoMethodParam(params = {})
    public PLiveCodingFeedback liveCodingFeedback() {
        PLiveCodingFeedback l = getFragment().liveCodingFeedback();
        l.enable = true;

        return l;
    }


    @ProtoMethod(description = "sends a name event with a json object", example = "")
    @ProtoMethodParam(params = {"name", "jsonObject"})
    public void sendEvent(String name, Object obj) {
        pevents.sendEvent(name, (NativeObject) obj);
    }


    @ProtoMethod(description = "receives a named event with a json object", example = "")
    @ProtoMethodParam(params = {"name", "function(name, jsonObject)"})
    public String listenEvent(String name, PEvents.EventCB callback) {
        return pevents.add(name, callback);
    }


    @ProtoMethod(description = "receives a named event with a json object", example = "")
    @ProtoMethodParam(params = {"name", "function(name, jsonObject)"})
    public void removeEvent(String id) {
        pevents.remove(id);
    }

//
//
//    @APIMethod(description = "", example = "")
//    @APIParam(params = { "fileName" })
//    public void openWithApp(final String src) {
//        final String projectPath = ProjectManager.getInstance().getCurrentProject().getStoragePath();
//
//        Intent intent = new Intent(Intent.ACTION_VIEW);
//        intent.setData(Uri.parse(projectPath + "/" + src));
//
//        mContext.startActivity(intent);
//    }


    @ProtoMethod(description = "opens a file with a given app provided as package name ", example = "")
    @ProtoMethodParam(params = {"fileName", "packageName"})
    public void openWithApp(final String src, String packageName) {
        final String projectPath = ProjectManager.getInstance().getCurrentProject().getStoragePath();

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + projectPath + "/" + src), packageName);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        getContext().startActivity(intent);
    }

}