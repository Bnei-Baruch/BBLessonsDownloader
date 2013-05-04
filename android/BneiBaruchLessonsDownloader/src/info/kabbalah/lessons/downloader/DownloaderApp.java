package info.kabbalah.lessons.downloader;

//import org.acra.ACRA;
//import org.acra.annotation.ReportsCrashes;

import android.app.Application;

//@ReportsCrashes(formKey = "dFNTemhCbldlX2lLZjZBV09yRHFMTFE6MQ")
public class DownloaderApp extends Application {
	@Override
    public void onCreate() {
        // The following line triggers the initialization of ACRA
        //ACRA.init(this);
        super.onCreate();
    }
}
