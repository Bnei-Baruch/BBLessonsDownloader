using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace BBLessonsDwnldApp
{
    class FDConstants
    {
        //public static string httpServerPath = "http://dl.dropbox.com/u/22519537/"; //my dropbox
        //public static string ftpServerPath = "http://dl.dropbox.com/u/3074981/";
        //public static string ftpServerPath = "http://upload.kbb1.com/lessondn/";
        public static string mylibraryLessonList = "http://old.kabbalahmedia.info/api/morning_lessons.json";
        public static string txtFileToday;
        public static string dateFormat = "yyyy-MM-dd";
        public static string[] languagesList = new string[] { "Hebrew", "English", "Russian", "Spanish", "German", "Italian", "French", "Turkish", "Swedish", "Portuguese" };
        public static string[] sizeList = new string[] { "Normal", "High" };
        public static string dirFolderName = "Morning Lessons";
        public static string connProblemsMsg = "Network connection problems. Please, contact your administrator";

    }
}
