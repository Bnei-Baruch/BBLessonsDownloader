using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;
using System.IO;
using BBLessonsDwnldApp;
using System.Threading;

namespace ServiceInstaller
{
    public partial class frmServiceDetails : Form
    {
        private string lessonsDir;
        private string language;

        public frmServiceDetails()
        {
            InitializeComponent();
            CreateMyDocumentsNewDirectory();
            textLessonsDirectory.Text = lessonsDir;

            comboInstLang.Items.AddRange(FDConstants.languagesList);
            comboInstLang.Text = "English";

        }

        private void CreateMyDocumentsNewDirectory()
        {
            lessonsDir = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments) + "\\" + FDConstants.dirFolderName;
            if (!Directory.Exists(lessonsDir))
                System.IO.Directory.CreateDirectory(lessonsDir);
        }


        public string LessonsDir
        {
            get
            {
                return lessonsDir;
            }
            set
            {
                lessonsDir = value;
            }
        }

        public string Language
        {
            get
            {
                return language;
            }
            set
            {
                language = value;
            }
        }

        private void btnSaveSettings_Click(object sender, EventArgs e)
        {
            lessonsDir = textLessonsDirectory.Text.Trim();
        }

        private void btnBrowse_Click(object sender, EventArgs e)  
        {  
            Thread thread = new Thread(new ThreadStart(BrowseDialog));  
            thread.SetApartmentState(ApartmentState.STA);  
            thread.Start();  
            thread.Join();

            if (lessonsDir != "") textLessonsDirectory.Text = lessonsDir;  
        }  
 
        private void BrowseDialog()  
        {  
            using (FolderBrowserDialog dlg = new FolderBrowserDialog())  
            {
                dlg.SelectedPath = lessonsDir;//Environment.SpecialFolder.MyDocuments.ToString();  
                dlg.RootFolder = Environment.SpecialFolder.Desktop;  
                dlg.ShowNewFolderButton = true;  
                if (dlg.ShowDialog() == DialogResult.OK)  
                {  
                    lessonsDir = dlg.SelectedPath;  
                }  
            }  
        }  
        
        private void comboInstLang_SelectedIndexChanged(object sender, EventArgs e)
        {
            language = (String)comboInstLang.SelectedItem;
        }
     }
}