using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Windows.Forms;
using System.IO;
using Westwind.Tools;

namespace BBDailyLessonsDownloader
{
    public partial class WebBrowserForm : Form
    {
        public WebBrowserForm()
        {
            InitializeComponent();
        }

        public void WebBrowserForm_Load(object sender, EventArgs e)
        {
            // *** Assumes the Html page is in a .\html folder beneath the EXE
            //string htmlPage = Path.GetFullPath(@".\html\gahtml.htm");
            //this.Browser.Navigate(htmlPage);
            this.Browser.Navigate("http://files.kbb1.com/dailylesson/gahtml.htm");
        }

        public void setGAStatistics(string url)
        {
            try
            {

                bool mp3 = false;
                mp3 = url.EndsWith("mp3");
                string trackevent = mp3 ? (url.EndsWith("k.mp3") ? "audio (high)" : "audio (normal size)") : (url.EndsWith("wmv")?"wmv":"mp4");

                this.CallJavascriptFunction("setGAStat", trackevent);
                //object[] args = { "test2" };
                //this.Browser.Document.InvokeScript("setGAStat",args);//, "test").ToString();
            }
            catch{}
        }

        private object CallJavascriptFunction(string function, params object[] parms)
        {
            // *** Get the COM DOM object (not the .NET Wrapper)
            object doc = this.Browser.Document.DomDocument;

            // *** Now you can use Reflection on the COM DOM
            object win = wwUtils.GetPropertyCom(doc, "parentWindow");

            // *** Call the JavaScript function and capture the result value
            object result = wwUtils.CallMethodCom(win, function, parms);

            return result;
        }

    }
}
