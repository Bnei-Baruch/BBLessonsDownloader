using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace BBLessonsDwnldApp
{
    public class ClientProperties
    {
        private string clientLanguage;
        private string currDirectory;
        private string todayFolderName;
        private string yesterdayFolderName;
        private string language;
        private string size;
        private string prefix;
        private string removableDeviceDirectory;
        private bool audiomp3;
        private bool videomp4;
        private bool video;
        private bool copyToRemovable;
        private bool removeOldFolders;

        public ClientProperties()
        {
            this.clientLanguage = "";
            this.currDirectory = "";
            this.todayFolderName = "";
            this.yesterdayFolderName = "";
            this.language = "";
            this.size = "";
            this.prefix = "";
            this.audiomp3 = true;
            this.videomp4 = false;
            this.video = false;
        }

        public string ClientLanguage
        {
            get { return clientLanguage; }
            set { clientLanguage = value; }
        }

        public string TodayFolderName
        {
            get { return todayFolderName; }
            set { todayFolderName = value; }
        }

        public string YesterdayFolderName
        {
            get { return yesterdayFolderName; }
            set { yesterdayFolderName = value; }
        }

        public string CurrDirectory
        {
            get { return currDirectory; }
            set { currDirectory = value; }
        }

        public string Language
        {
            get { return language; }
            set
            {
                language = value;
                SetPefix();
            }
        }

        public string Size
        {
            get { return size; }
            set { size = value; }
        }

        public string Prefix
        {
            get { return prefix; }
            set { prefix = value; }
        }

        public bool Audiomp3
        {
            get { return audiomp3; }
            set { audiomp3 = value; }
        }

        public bool Videomp4
        {
            get { return videomp4; }
            set { videomp4 = value; }
        }

        public bool Video
        {
            get { return video; }
            set { video = value; }
        }

        public bool CopyToRemovable
        {
            get { return copyToRemovable; }
            set { copyToRemovable = value; }
        }

        public bool RemoveOldFolders
        {
            get { return removeOldFolders; }
            set { removeOldFolders = value; }
        }

        public string RemovableDeviceDirectory
        {
            get { return removableDeviceDirectory; }
            set { removableDeviceDirectory = value; }
        }

        public void SetPefix()
        {
            switch (this.language)
            {
                case "Hebrew":
                    this.prefix = "heb_";
                    break;
                case "English":
                    this.prefix = "eng_";
                    break;
                case "Russian":
                    this.prefix = "rus_";
                    break;
                case "Spanish":
                    this.prefix = "spa_";
                    break;
                case "German":
                    this.prefix = "ger_";
                    break;
                case "Italian":
                    this.prefix = "ita_";
                    break;
                case "French":
                    this.prefix = "fre_";
                    break;
                case "Turkish":
                    this.prefix = "trk_";
                    break;
                case "Swedish":
                    this.prefix = "swe_";
                    break;
            }
        }
    }
}
