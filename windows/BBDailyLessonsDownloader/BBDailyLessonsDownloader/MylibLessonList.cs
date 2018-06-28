using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Runtime.Serialization;

namespace BBDailyLessonsDownloader
{
    [DataContract]
    public class LessonInfo
    {
        [DataMember(Name = "id")]
        public int id { get; set; }

        [DataMember(Name = "name")]
        public string name { get; set; }

        [DataMember(Name = "updated")]
        public string updated { get; set; }

        [DataMember(Name = "url")]
        public string url { get; set; }

        [DataMember(Name = "size")]
        public int size { get; set; }

        [DataMember(Name = "type")]
        public string type { get; set; }
    }

    [DataContract]
    public class LessonListForDate
    {
        [DataMember(Name = "date")]
        public string date { get; set; }

        [DataMember(Name = "files")]
        public LessonInfo[] files { get; set; }
    }

    [DataContract]
    public class LessonListForLang
    {
        [DataMember(Name = "lang")]
        public string lang { get; set; }

        [DataMember(Name = "dates")]
        public LessonListForDate[] dates { get; set; }
    }

    [DataContract]
    public class MylibJSONLessonlist
    {
        [DataMember(Name = "morning_lessons")]
        public LessonListForLang morning_lessons { get; set; }
    }
}
