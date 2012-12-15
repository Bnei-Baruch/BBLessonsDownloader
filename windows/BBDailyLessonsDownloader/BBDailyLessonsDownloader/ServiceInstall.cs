using System;
using System.ComponentModel;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;
using System.Configuration.Install;
using System.Windows.Forms;
using System.IO;
using System.Reflection;
using System.ServiceProcess;
using System.Resources;
using System.Threading;
using System.Management;
using System.Collections;
using Microsoft.Win32;
using System.Runtime.InteropServices;
using BBDailyLessonsDownloader.Properties;
using BBLessonsDwnldApp;
using System.Configuration;
using System.Xml;

namespace ServiceInstaller
{
    [RunInstaller(true)]
    public partial class ServiceInstall : System.Configuration.Install.Installer
    {

        public ServiceInstall()
        {
            InitializeComponent();
        }

        public override void Install(IDictionary savedState)
        {
            base.Install(savedState);
            frmServiceDetails newfrm = new frmServiceDetails();
            string exePath = getConfigFilePath();
            XmlDocument doc = new XmlDocument();
            doc.Load(exePath);
            XmlNode settingsNode = doc.GetElementsByTagName("userSettings")[0];
           
            newfrm.ShowDialog();
            newfrm.Focus();
            if(newfrm.DialogResult == DialogResult.OK)
            {
                XmlNode node = settingsNode.SelectSingleNode(String.Format("BBDailyLessonsDownloader.Properties.Settings/setting[@name='{0}']", "language"));
                if (node != null)
                {
                    XmlNode valueNode = null;
                    valueNode = node.SelectSingleNode("value");

                    if (valueNode != null)
                    {
                        valueNode.FirstChild.Value = newfrm.Language;
                    }
                }

                node = settingsNode.SelectSingleNode(String.Format("BBDailyLessonsDownloader.Properties.Settings/setting[@name='{0}']", "currDirectory"));
                if (node != null)
                {
                    XmlNode valueNode = null;
                    valueNode = node.SelectSingleNode("value");

                    if (valueNode != null)
                    {
                        valueNode.FirstChild.Value = newfrm.LessonsDir;
                    }
                }

                node = settingsNode.SelectSingleNode(String.Format("BBDailyLessonsDownloader.Properties.Settings/setting[@name='{0}']", "isFirsTime"));
                if (node != null)
                {
                    XmlNode valueNode = null;
                    valueNode = node.SelectSingleNode("value");

                    if (valueNode != null)
                    {
                        valueNode.FirstChild.Value = "True";
                    }
                }

            }
            doc.Save(exePath);
        }

        public override void Uninstall(IDictionary savedState)
        {
            base.Uninstall(savedState);
            //RegistrationServices regSrv = new RegistrationServices();
            //egSrv.UnregisterAssembly(base.GetType().Assembly);

            RegistryKey rkApp = Registry.CurrentUser.OpenSubKey("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run", true);
            rkApp.DeleteValue("BB DailyLessons Downloader", false);

        }

        public override void Commit(IDictionary savedState)
        {
            base.Commit(savedState);
        }

        protected override void OnCommitted(IDictionary savedState)
        {
            base.OnCommitted(savedState);

            string exePath = Assembly.GetExecutingAssembly().Location;

            RegistryKey rkApp = Registry.CurrentUser.OpenSubKey("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Run", true);
            rkApp.SetValue("BB DailyLessons Downloader", exePath);
          
            System.Diagnostics.Process proc = new System.Diagnostics.Process();
            InstallContext cont = this.Context;
            ProcessStartInfo inf = new ProcessStartInfo(cont.Parameters["AssemblyPath"]);
            proc.StartInfo = inf;
            proc.Start();
        }


        private static string getConfigFilePath()
        {
            return Assembly.GetExecutingAssembly().Location + ".config";
        }

     }  
 }
