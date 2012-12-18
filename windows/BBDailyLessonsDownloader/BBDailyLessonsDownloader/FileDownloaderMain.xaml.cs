﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using System.Windows.Forms;
using System.IO;
using System.Xml;
using BBDailyLessonsDownloader.Properties;
using System.Diagnostics;
using System.Net.NetworkInformation;
using System.ComponentModel;
using System.Reflection;
using JCMLib;
using iTuner;
using System.Windows.Interop;
using Dolinay;
using System.Configuration;
using System.Runtime.Remoting.Contexts;
using System.Threading;
using BBDailyLessonsDownloader;
using System.Web;

namespace BBLessonsDwnldApp
{
    /// <summary>
    /// Interaction logic for FileDownloaderMain.xaml
    /// </summary>
    public partial class FileDownloaderMain : Window
    {
        // Creating a new instance of a FileDownloader
        private FileDownloader downloader = new FileDownloader();
        // Creating a new instance of Removable Device Detector
        private DriveDetector driveDetector = new DriveDetector();
        // Creating a new instance of Client Properties
        private ClientProperties clientProps = new ClientProperties();
        // Creating a new instance of Notify Icon
        NotifyIconEx notifyIconEx = new NotifyIconEx();

        #region Fields
        private string m_removableDir = "";
        private bool m_removableDownloadInProcess = false;
        private bool m_removableDownloadCompleted = false;
        private bool m_canStartDownloadToRemovable = false;
        #endregion

        private FolderBrowserDialog folderBrowserDialog1 = new FolderBrowserDialog();
        private OpenFileDialog openFileDialog1 = new OpenFileDialog();

        /// <summary>Occurs every time a lessons can be downloaded to removable device</summary>
        private event EventHandler CanStartDownloadToRemovable;

        private WebBrowserForm webBrowserFrm = null;
        //System.Windows.Forms.Timer timer;
        private System.Timers.Timer timer;

        public FileDownloaderMain()
        {
            Process[] theProcesses = System.Diagnostics.Process.GetProcessesByName(System.Windows.Forms.Application.ProductName);
            if (theProcesses.Length > 1)
            {
                //System.Windows.MessageBox.Show("There is already another instance of the application running");
                System.Environment.Exit(1);
            }

            InitializeComponent();

            #region Events Handlers
            CommandBindings.Add(new CommandBinding(ApplicationCommands.Close,
                new ExecutedRoutedEventHandler(delegate(object sender, ExecutedRoutedEventArgs args) { this.Close(); })));

            downloader.StateChanged += new EventHandler(downloader_StateChanged);
            downloader.CalculatingFileSize += new FileDownloader.CalculatingFileSizeEventHandler(downloader_CalculationFileSize);
            downloader.ProgressChanged += new EventHandler(downloader_ProgressChanged);
            downloader.FileDownloadAttempting += new EventHandler(downloader_FileDownloadAttempting);
            downloader.FileDownloadStarted += new EventHandler(downloader_FileDownloadStarted);
            downloader.FileDownloadSucceeded += new EventHandler(downloader_FileDownloadSucceeded);
            downloader.Completed += new EventHandler(downloader_Completed);
            downloader.CancelRequested += new EventHandler(downloader_CancelRequested);
            downloader.DeletingFilesAfterCancel += new EventHandler(downloader_DeletingFilesAfterCancel);
            downloader.Canceled += new EventHandler(downloader_Canceled);
            downloader.FileDownloadFailed += new FileDownloader.FailEventHandler(downloader_FileDownloadError);
            NetworkStatus.AvailabilityChanged += new NetworkStatusChangedHandler(DoAvailabilityChanged);
            driveDetector.DeviceArrived += new DriveDetectorEventHandler(OnDriveArrived);
            driveDetector.DeviceRemoved += new DriveDetectorEventHandler(OnDriveRemoved);
            CanStartDownloadToRemovable += new EventHandler(StartDownload_Removable);
            notifyIconEx.Click += new EventHandler(OnClickIcon);
            #endregion

            #region Initializations
            ExeConfigurationFileMap map = new ExeConfigurationFileMap();
            map.ExeConfigFilename = Assembly.GetExecutingAssembly().Location + ".config";
            Configuration conf = ConfigurationManager.OpenMappedExeConfiguration(map, ConfigurationUserLevel.None);
            ConfigurationSectionGroup appSettingsGroup = conf.GetSectionGroup("userSettings");
            ClientSettingsSection clientSettings = (ClientSettingsSection)appSettingsGroup.Sections["BBDailyLessonsDownloader.Properties.Settings"];
            if (clientSettings != null)
            {
                string searchName = "isFirsTime";

                foreach (SettingElement setting in clientSettings.Settings)
                {
                    string value = setting.Value.ValueXml.InnerText;
                    string name = setting.Name;
                    if (name.ToLower().StartsWith(searchName.ToLower()))
                    {
                        if (value == "True")
                        {
                            setting.Value.ValueXml.InnerText = "False";
                            clientSettings.SectionInformation.ForceSave = true;
                            conf.Save();
                            Settings.Default.Reset();
                            Settings.Default.Reload();
                        }
                    }
                }
            }
            if (NetworkStatus.IsAvailable)
                downloader.IsNetworkAvailable = true;
            else
                downloader.IsNetworkAvailable = false;

            InitializeClientProps();
            InitializeDefaults();
            InitializeTimer();
            #endregion

            #region Set Icon
            System.IO.Stream iconStream = System.Windows.Application.GetResourceStream(new Uri("pack://application:,,/images/favicon.ico")).Stream;
            notifyIconEx.Text = "BB Daily Lesson Downloader\n  (Click to open window)";
            notifyIconEx.Icon = new System.Drawing.Icon(iconStream);
            notifyIconEx.Visible = true;
            #endregion

            this.WindowState = WindowState.Normal;
            checkForNewVersion();
            startDownloadProcess();
        }

        private void checkForNewVersion()
        {
            string downloaderUrl = "";
            Version newVersion = null;
            string xmlUrl = "http://files.kbb1.com/dailylesson/Upgrade.xml";
            XmlTextReader reader = null;
            try
            {
                reader = new XmlTextReader(xmlUrl);
                reader.MoveToContent();
                string elementName = "";
                if ((reader.NodeType == XmlNodeType.Element) && reader.Name == "downloaderapp")
                {
                    while (reader.Read())
                    {
                        if (reader.NodeType == XmlNodeType.Element)
                            elementName = reader.Name;
                        else
                        {
                            if ((reader.NodeType == XmlNodeType.Text) && reader.HasValue)
                            {
                                switch (elementName)
                                {
                                    case "version":
                                        newVersion = new Version(reader.Value);
                                        break;
                                    case "url":
                                        downloaderUrl = reader.Value;
                                        break;
                                }
                            }
                        }
                    }
                }
            }
            catch
            {
            }
            finally
            {
                if (reader != null)
                    reader.Close();
            }
            Version applicationVersion = System.Reflection.Assembly.GetExecutingAssembly().GetName().Version;
            if (applicationVersion.CompareTo(newVersion) < 0)
            {
                if (System.Windows.MessageBox.Show("Version " + newVersion.ToString() + " of BB Lesson Downloader is now available, would you like to download it?", "Version Upgrage", MessageBoxButton.YesNo) == MessageBoxResult.Yes)
                {
                    System.Diagnostics.Process.Start(downloaderUrl);
                    System.Environment.Exit(1);
                }
                else
                {
                    return;
                }
            }
        }


        #region Components Events Handlers
        private void timer_Tick(object sender, System.EventArgs e)
        {
            if (downloader.CanStart && NetworkStatus.IsAvailable)
                startDownloadProcess();
        }

        private void timer_Elapsed(object sender, System.Timers.ElapsedEventArgs e)
        {
            //timer.Stop();
            if (downloader.CanStart && NetworkStatus.IsAvailable)
                startDownloadProcess();
        }

        private void OnClickIcon(object sender, EventArgs e)
        {
            this.Show();
            this.WindowState = WindowState.Normal;
        }

        //Start Download Process
        private void btnStart_Click(object sender, RoutedEventArgs e)
        {
            if (NetworkStatus.IsAvailable)
            {
                if (clientProps.RemoveOldFolders)
                    deleteOldDirectories();

                createFolders();
                ShowLocalFilesList();
                // Set the path to the local directory where the files will be downloaded to
                downloader.LocalDirectory = clientProps.CurrDirectory + "\\" + clientProps.TodayFolderName; //localDir;//openFolderDialog.SelectedPath;
                // Clear the current list of files (in case it's not the first download)
                downloader.Files.Clear();
                pBarFileProgress.Value = 0;
                pBarTotalProgress.Value = 0;
                lblFileProgress.Content = "";
                lblTotalProgress.Content = "";
                lblStatus.Content = "";

                string[] remoteFiles = getFilesListFromXML(false);
                string[] localFiles = GetLocalFilesList(false);

                string[] filesToDownload = GetRemoteLocalFilesDiff(remoteFiles, localFiles);
                if (filesToDownload == null)
                {
                    lblStatus.Content = "No new Lessons Found";
                    return;
                }

                if (webBrowserFrm == null)
                {
                    webBrowserFrm = new WebBrowserForm();
                    webBrowserFrm.WebBrowserForm_Load(new object[] { null }, new EventArgs { });
                }
                //System.Windows.Forms.Application.Run(webBrowserFrm);
                
                // Get the contents of the rich text box
                foreach (string fl in filesToDownload)
                {
                    // Note: check if the url is valid before adding it, and probably should do this is a real application
                    downloader.Files.Add(new FileDownloader.FileInfo(FDConstants.ftpServerPath + fl));
                }

                // Start the downloader
                downloader.Start();
            }
            else
            {
                ShowLocalFilesList();
                lblStatusTreadsHandler(lblStatus, FDConstants.connProblemsMsg);
            }
        }

        //Start Download to removable device
        private void StartDownload_Removable(object sender, EventArgs e)
        {
            if (NetworkStatus.IsAvailable)
            {
                if (m_canStartDownloadToRemovable && clientProps.CopyToRemovable && downloader.CanStart)
                {
                    string directoryString = @"" + (m_removableDir + FDConstants.dirFolderName + "\\" + clientProps.TodayFolderName);
                    try
                    {
                        DirectoryInfo di = new DirectoryInfo(@"" + m_removableDir);

                        if (!Directory.Exists(directoryString))
                            System.IO.Directory.CreateDirectory(directoryString);
                        clientProps.RemovableDeviceDirectory = directoryString;
                        if (downloader.CanStart)
                            StartDownloadToRemovable();
                    }
                    catch (Exception ex)
                    {
                        lblStatusTreadsHandler(lblStatus, "The process failed: " + ex.ToString());
                    }
                    finally { }
                }
            }
            else
            {
                lblStatusTreadsHandler(lblStatus, FDConstants.connProblemsMsg);
            }
        }

        private void linkLocalDir_Clicked(object sender, RoutedEventArgs e)
        {
            folderBrowserDialog1.SelectedPath = linkLocalDir.Content.ToString();
            linkLocalDir.Content = folderBrowserDialog1.SelectedPath;
            if (folderBrowserDialog1.ShowDialog() == System.Windows.Forms.DialogResult.OK)
            {
                linkLocalDir.Content = folderBrowserDialog1.SelectedPath;
                clientProps.CurrDirectory = folderBrowserDialog1.SelectedPath;
            }
            else
            {
                return;
            }
        }

        private void btnToday_Click(object sender, RoutedEventArgs e)
        {
            string todayFolder = clientProps.CurrDirectory + "\\" + clientProps.TodayFolderName;
            createFolders();
            Process.Start(@todayFolder);
        }

        private void btnYesterday_Click(object sender, RoutedEventArgs e)
        {
            string yesterdayFolder = clientProps.CurrDirectory + "\\" + clientProps.YesterdayFolderName;
            createFolders();
            Process.Start(yesterdayFolder);
        }

        private void btnContactUs_Click(object sender, RoutedEventArgs e)
        {
            SendMail sendMailDialog = new SendMail();
            sendMailDialog.ShowDialog();
        }

        private void btnExit_Click(object sender, RoutedEventArgs e)
        {
            this.Close();
        }

        private void btnDonate_Click(object sender, RoutedEventArgs e)
        {
            var psi = new System.Diagnostics.ProcessStartInfo("http://www.kabbalah.info/donations/");
            psi.UseShellExecute = true;
            psi.Verb = "open";

            System.Diagnostics.Process.Start(psi);

        }

        private void comboLang_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            System.Windows.Controls.ComboBoxItem curItem = ((System.Windows.Controls.ComboBoxItem)comboLang.SelectedItem);
            clientProps.Language = curItem.Content.ToString();
        }

        private void comboSize_SelectionChanged(object sender, SelectionChangedEventArgs e)
        {
            System.Windows.Controls.ComboBoxItem curItem = ((System.Windows.Controls.ComboBoxItem)comboSize.SelectedItem);
            clientProps.Size = curItem.Content.ToString();
        }

        private void chkVideo_Checked(object sender, RoutedEventArgs e)
        {
            Boolean _video = (Boolean)chkVideo.IsChecked;
            Boolean _audio = (Boolean)chkAudiomp3.IsChecked;
            //Boolean _vmp4 = (Boolean)chkVideomp4.IsChecked;
            if (!_audio && !_video
                //&& !_vmp4
                )
            {
                chkAudiomp3.IsChecked = true;
                clientProps.Audiomp3 = true;
            }
            clientProps.Videomp4 = (Boolean)chkVideo.IsChecked;
//            clientProps.Video = (Boolean)chkVideo.IsChecked;
        }

        private void chkAudio_Checked(object sender, RoutedEventArgs e)
        {
            Boolean _video = (Boolean)chkVideo.IsChecked;
            Boolean _audio = (Boolean)chkAudiomp3.IsChecked;
            //Boolean _vmp4 = (Boolean)chkVideomp4.IsChecked;
            if (!_audio && !_video
                //&& !_vmp4
                )
            {
                chkVideo.IsChecked = true;
                clientProps.Video = true;
            }
            clientProps.Audiomp3 = (Boolean)chkAudiomp3.IsChecked;
        }

        //private void chkVideoMP4_Checked(object sender, RoutedEventArgs e)
        //{
        //    Boolean _video = (Boolean)chkVideo.IsChecked;
        //    Boolean _audio = (Boolean)chkAudiomp3.IsChecked;
        //    //Boolean _vmp4 = (Boolean)chkVideomp4.IsChecked;
        //    if (!_audio && !_video && !_vmp4)
        //    {
        //        chkAudiomp3.IsChecked = true;
        //        clientProps.Audiomp3 = true;
        //    }
        //    clientProps.Videomp4 = (Boolean)chkVideomp4.IsChecked;
        //}

        private void chkCopyToRemovable_Checked(object sender, RoutedEventArgs e)
        {
            clientProps.CopyToRemovable = (Boolean)chkCopyToRemovable.IsChecked;
        }

        private void chkRemoveOld_Checked(object sender, RoutedEventArgs e)
        {
            clientProps.RemoveOldFolders = (Boolean)chkRemoveOld.IsChecked;
        }

        private void lstFiles_DoubleClick(object sender, RoutedEventArgs e)
        {
            if (lstFiles.SelectedItem == null) return;
            string fName = lstFiles.SelectedItem.ToString();
            ProcessStartInfo psi = new ProcessStartInfo();//ProccessStartInfo(); 
            psi.FileName = clientProps.CurrDirectory + "\\" + fName;//"myfile.txt"; 
            if (System.IO.File.Exists(psi.FileName))
            {
                Process p = new Process();
                p.StartInfo = psi;
                p.Start();
            }
        }

        private void Window_StateChanged(object sender, EventArgs e)
        {
            if (this.WindowState == WindowState.Minimized)
                this.Hide();
        }


        private void DoAvailabilityChanged(object sender, NetworkStatusChangedArgs e)
        {
            if (NetworkStatus.IsAvailable)
            {
                downloader.IsNetworkAvailable = true;
                if (downloader.CanStart)
                    startDownloadProcess();
            }
            else
            {
                downloader.IsNetworkAvailable = false;
                lblStatusTreadsHandler(lblStatus, FDConstants.connProblemsMsg);
            }
        }

        // Close the window when the close button is hit
        private void btnClose_Click(object sender, RoutedEventArgs e)
        {
            if (System.Windows.MessageBox.Show("Are you sure you want to quit?", "Exit", MessageBoxButton.YesNo) == MessageBoxResult.Yes)
            {
                System.Environment.Exit(1);
                //this.Close();
            }
            else
            {
                return;
            }
        }

        protected override void OnClosing(CancelEventArgs e)
        {
            if (System.Windows.MessageBox.Show("Are you sure you want to quit?", "Exit", MessageBoxButton.YesNo) == MessageBoxResult.Yes)
            {
                if (downloader.CanStop)
                    downloader.Stop();
                base.OnClosing(e);
                SetUserProperies();
            }
            else
            {
                e.Cancel = true;
            }
        }

        // Called by DriveDetector when removable device in inserted 
        private void OnDriveArrived(object sender, DriveDetectorEventArgs e)
        {
            // e.Drive is the drive letter for the device which just arrived, e.g. "E:\\"
            lblRemovable.Content = "Removable Device Connected: " + e.Drive;
            m_removableDir = e.Drive;

            m_canStartDownloadToRemovable = true;

            if (clientProps.CopyToRemovable)
            {
                if (CanStartDownloadToRemovable != null) { this.CanStartDownloadToRemovable(this, new EventArgs()); }
            }
        }

        // Called by DriveDetector after removable device has been unpluged 
        private void OnDriveRemoved(object sender, DriveDetectorEventArgs e)
        {
            // TODO: do clean up here, etc. Letter of the removed drive is in e.Drive;
            lblRemovable.Content = "<No Removable Device Connected>";
            clientProps.RemovableDeviceDirectory = "";
            m_removableDir = "";
            m_removableDownloadInProcess = false;
            m_canStartDownloadToRemovable = false;
            m_removableDownloadCompleted = false;
        }
        #endregion

        #region Download process handle Events
        private void btnPause_Click(object sender, RoutedEventArgs e)
        {
            // Pause downloader
            downloader.Pause();
        }

        private void btnResume_Click(object sender, RoutedEventArgs e)
        {
            // Resume downloader
            downloader.Resume();
        }

        private void btnStop_Click(object sender, RoutedEventArgs e)
        {
            // Stop downloader
            // Note: This will not be instantantanious - the current requests need to be closed down, and the downloaded files need to be deleted
            downloader.Stop();
        }

        // This event is fired every time the paused or busy state is changed, and used here to set the controls of the interface
        // This makes it enuivalent to a void handling both downloader.IsBusyChanged and downloader.IsPausedChanged
        private void downloader_StateChanged(object sender, EventArgs e)
        {
            // Setting the buttons
            btnStart.IsEnabled = downloader.CanStart && !downloader.HasBeenCanceled;
            btnStop.IsEnabled = downloader.CanStop;
            btnPause.IsEnabled = downloader.CanPause;
            btnResume.IsEnabled = downloader.CanResume;
        }

        private void checkIsDwnldToRemovableAvlbl()
        {            // If downloads were comleted or stoped
            if (downloader.CanStart && m_canStartDownloadToRemovable)
            {
                // In case last download was to a removable device,
                // Reset removable device states
                if (m_removableDownloadInProcess)
                {
                    m_removableDownloadCompleted = true;
                    m_removableDownloadInProcess = false;
                    lblStatusTreadsHandler(lblRemovable, "Removable Device Connected: " + m_removableDir);
                }
                else if (!m_removableDownloadCompleted)
                    if (CanStartDownloadToRemovable != null) { this.CanStartDownloadToRemovable(this, new EventArgs()); }
            }
        }
        // Show the progress of file size calculation
        // Note that these events will only occur when the total file size is calculated in advance, in other words when the SupportsProgress is set to true
        private void downloader_CalculationFileSize(object sender, Int32 fileNr)
        {
            lblStatus.Content = String.Format("Calculating file sizes - file {0} of {1}", fileNr, downloader.Files.Count);
        }

        // Occurs every time of block of data has been downloaded, and can be used to display the progress with
        // Note that you can also create a timer, and display the progress every certain interval
        // Also note that the progress properties return a size in bytes, which is not really user friendly to display
        //      The FileDownloader class provides static functions to format these byte amounts to a more readible format, either in binary or decimal notation 
        private void downloader_ProgressChanged(object sender, EventArgs e)
        {
            pBarFileProgress.Value = downloader.CurrentFilePercentage();
            lblFileProgress.Content = String.Format("Downloaded {0} of {1} ({2}%)", FileDownloader.FormatSizeBinary(downloader.CurrentFileProgress), FileDownloader.FormatSizeBinary(downloader.CurrentFileSize), downloader.CurrentFilePercentage()) + String.Format(" - {0}/s", FileDownloader.FormatSizeBinary(downloader.DownloadSpeed));

            if (downloader.SupportsProgress)
            {
                pBarTotalProgress.Value = downloader.TotalPercentage();
                lblTotalProgress.Content = String.Format("Downloaded {0} of {1} ({2}%)", FileDownloader.FormatSizeBinary(downloader.TotalProgress), FileDownloader.FormatSizeBinary(downloader.TotalSize), downloader.TotalPercentage());
            }
        }

        // This will be shown when the request for the file is made, before the download starts (or fails)
        private void downloader_FileDownloadAttempting(object sender, EventArgs e)
        {
            lblStatus.Content = String.Format("Preparing {0}", downloader.CurrentFile.Name);
        }

        // Add recent downloaded file to the list box  
        private void downloader_FileDownloadSucceeded(object sender, EventArgs e)
        {
            if (!m_removableDownloadInProcess)
                lstFiles.Items.Add(clientProps.TodayFolderName + "\\" + downloader.recentFileDownloaded);
        }

        // Display of the file info after the download started
        private void downloader_FileDownloadStarted(object sender, EventArgs e)
        {
            webBrowserFrm.setGAStatistics(downloader.CurrentFile.Name);
            lblStatus.Content = String.Format("Downloading {0}", downloader.CurrentFile.Name);
        }

        private void downloader_FileDownloadError(object sender, Exception e)
        {
            var newEventArgs = new RoutedEventArgs(System.Windows.Controls.Button.ClickEvent);

            if (!btnStop.Dispatcher.CheckAccess())
            {
                btnStop.Dispatcher.BeginInvoke(
                 System.Windows.Threading.DispatcherPriority.Normal,
                 new Action(
                   delegate()
                   {
                       btnStop.RaiseEvent(newEventArgs);
                   }
               ));
            }
            else
            {
                btnStop.RaiseEvent(newEventArgs);
            }

            string msg = (e == null) ? FDConstants.connProblemsMsg : e.Message;
            lblStatusTreadsHandler(lblStatus, msg);
        }

        // Display of a completion message, showing the amount of files that has been downloaded.
        // Note, this does not hold into account any possible failed file downloads
        private void downloader_Completed(object sender, EventArgs e)
        {
            lblStatus.Content = String.Format("Download completed, downloaded {0} files.", downloader.Files.Count);
            notifyIconEx.ShowBalloon("New Lessons are available", "New lessons have been downloaded to daily folder!", NotifyIconEx.NotifyInfoFlags.Info, 10);
            checkIsDwnldToRemovableAvlbl();
        }

        // Show a message that the downloads are being canceled - all files downloaded will be deleted and the current ones will be aborted
        private void downloader_CancelRequested(object sender, EventArgs e)
        {
            lblStatusTreadsHandler(lblStatus, "Canceling downloads...");
        }

        // Show a message that the downloads are being canceled - all files downloaded will be deleted and the current ones will be aborted
        private void downloader_DeletingFilesAfterCancel(object sender, EventArgs e)
        {
            lblStatusTreadsHandler(lblStatus, "Canceling downloads - deleting files...");
        }

        // Show a message saying the downloads have been canceled
        private void downloader_Canceled(object sender, EventArgs e)
        {
            lblStatusTreadsHandler(lblStatus, "Download(s) canceled");
            pBarFileProgress.Value = 0;
            pBarTotalProgress.Value = 0;
            lblFileProgress.Content = "";
            lblTotalProgress.Content = "";
            ShowLocalFilesList();
            checkIsDwnldToRemovableAvlbl();
        }
        #endregion

        #region Initialization Functions
        private void InitializeDefaults()
        {
            linkLocalDir.Content = clientProps.CurrDirectory;
            string localDir = clientProps.CurrDirectory + "\\" + clientProps.TodayFolderName; ;
            ComboBoxItem cboxitem;
            foreach (string val in FDConstants.languagesList)
            {
                cboxitem = new ComboBoxItem();
                cboxitem.Content = val;
                comboLang.Items.Add(cboxitem);
            }
            comboLang.Text = clientProps.Language;
            foreach (string val in FDConstants.sizeList)
            {
                cboxitem = new ComboBoxItem();
                cboxitem.Content = val;
                comboSize.Items.Add(cboxitem);
            }
            comboSize.Text = clientProps.Size;
            chkAudiomp3.IsChecked = clientProps.Audiomp3;
            //chkVideomp4.IsChecked = clientProps.Videomp4;
            chkVideo.IsChecked = clientProps.Videomp4;
            chkCopyToRemovable.IsChecked = clientProps.CopyToRemovable;
            chkRemoveOld.IsChecked = clientProps.RemoveOldFolders;
        }

        private void InitializeClientProps()
        {
            if (Settings.Default.currDirectory == "null")
                clientProps.CurrDirectory = GetAndCreateMyDocumentsNewDirectory();
            else
                clientProps.CurrDirectory = Settings.Default.currDirectory;
            clientProps.Language = Settings.Default.language; //comboLang.Text;
            clientProps.SetPefix();
            clientProps.Size = Settings.Default.size;
            clientProps.Audiomp3 = Settings.Default.audiomp3;
            //clientProps.Videomp4 = Settings.Default.videomp4;
            clientProps.Video = Settings.Default.video;
            clientProps.CopyToRemovable = Settings.Default.copyToRemovable;
            clientProps.RemoveOldFolders = Settings.Default.removeOldFolders;
        }

        private void InitializeTimer()
        {
            this.timer = new System.Timers.Timer();
            this.timer.Interval = 60000 * 60; //every 1 hour
            this.timer.AutoReset = true;
            this.timer.Elapsed += new System.Timers.ElapsedEventHandler(timer_Elapsed);//this.timer_Tick);
            this.timer.Start();
        }
        #endregion

        #region Private Functions
        private void setFoldersName()
        {
            System.DateTime today = System.DateTime.Now;              // Use current time
            clientProps.TodayFolderName = today.ToString(FDConstants.dateFormat);
            System.DateTime yesterday = today.AddDays(-1);
            clientProps.YesterdayFolderName = yesterday.ToString(FDConstants.dateFormat);
            FDConstants.txtFileToday = clientProps.TodayFolderName.Replace("-", "") + ".txt";
        }

        private string GetAndCreateMyDocumentsNewDirectory()
        {
            string directoryString = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments) + "\\" + FDConstants.dirFolderName;
            if (!Directory.Exists(directoryString))
                System.IO.Directory.CreateDirectory(directoryString);

            return directoryString;
        }

        private void ShowLocalFilesList()
        {
            StringBuilder result = new StringBuilder();
            lstFiles.Items.Clear();

            try
            {
                DirectoryInfo di = new DirectoryInfo(clientProps.CurrDirectory + "\\" + clientProps.TodayFolderName);
                FileInfo[] rgFiles = di.GetFiles();
                if (rgFiles.Length > 0)
                {
                    foreach (FileInfo fi in rgFiles)
                    {
                        lstFiles.Items.Add(clientProps.TodayFolderName + "\\" + fi.Name);
                    }
                }
            }
            catch (Exception ex)
            {
                System.Windows.Forms.MessageBox.Show(ex.Message);
            }
        }

        private string[] GetRemoteLocalFilesDiff(string[] remoteFiles, string[] localFiles)
        {
            StringBuilder result = new StringBuilder();

            if (remoteFiles != null && remoteFiles.Length > 0)
            {
                if (localFiles != null)
                {
                    foreach (string tmpFile in remoteFiles)
                    {
                        string found = Array.Find(localFiles, item => item.Contains(tmpFile));
                        if (string.IsNullOrEmpty(found))
                        {
                            result.Append(tmpFile);
                            result.Append("\n");
                        }
                    }
                    if (result.Length > 0)
                    {
                        result.Remove(result.ToString().LastIndexOf('\n'), 1);
                        return result.ToString().Split('\n');
                    }
                }
                else return remoteFiles;
            }
            return null;
        }

        private string[] GetLocalFilesList(bool removable)
        {
            StringBuilder result = new StringBuilder();
            try
            {
                DirectoryInfo di = new DirectoryInfo(!removable ? (clientProps.CurrDirectory + "\\" + clientProps.TodayFolderName) : (clientProps.RemovableDeviceDirectory));
                FileInfo[] rgFiles = di.GetFiles();
                if (rgFiles.Length == 0)
                    return null;
                foreach (FileInfo fi in rgFiles)
                {
                    if (fi.Name.StartsWith(clientProps.Prefix))
                    {
                        result.Append(fi.Name);
                        result.Append("\n");
                    }
                }
                if (result.Length > 0)
                {
                    result.Remove(result.ToString().LastIndexOf("\n"), 1);
                    return result.ToString().Split('\n');
                }

                return null;
            }
            catch (Exception ex)
            {
                System.Windows.Forms.MessageBox.Show(ex.Message);
                return null;
            }
        }

        private string[] getFilesListFromXML(bool removable)
        {
            StringBuilder result = new StringBuilder();
            string fileToread = FDConstants.ftpServerPath + FDConstants.txtFileToday;
            XmlTextReader reader = new XmlTextReader(fileToread);
            XmlNodeType type;
            try
            {
                if (!removable)
                {
                    while (reader.Read())
                    {
                        type = reader.NodeType;
                        if (type == XmlNodeType.Element)
                        {
                            if (reader.Name == "File")
                            {
                                if (reader.HasAttributes)
                                {
                                    string fName = reader.GetAttribute("Name");
                                    if (fName.StartsWith(clientProps.Prefix))
                                    {
                                        bool _video = (clientProps.Videomp4 && fName.EndsWith("mp4"));
                                        //bool _video = ((clientProps.Video && fName.EndsWith("wmv")) || (clientProps.Videomp4 && fName.EndsWith("mp4")));
                                        bool _audio = (clientProps.Audiomp3 && (clientProps.Size == "High" && fName.EndsWith("k.mp3") || (clientProps.Size == "Normal" && fName.EndsWith("mp3") && !fName.EndsWith("k.mp3"))));
                                        if ((_audio || _video))
                                        {
                                            result.Append(fName);
                                            result.Append("\n");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                else
                {
                    while (reader.Read())
                    {
                        type = reader.NodeType;
                        if (type == XmlNodeType.Element)
                        {
                            if (reader.Name == "File")
                            {
                                if (reader.HasAttributes)
                                {
                                    string fName = reader.GetAttribute("Name");
                                    bool _mp3 = !clientProps.Videomp4 && ((clientProps.Size == "High" && fName.EndsWith("k.mp3") || (clientProps.Size == "Normal" && fName.EndsWith("mp3") && !fName.EndsWith("k.mp3"))));
                                    bool _mp4 = clientProps.Videomp4 && fName.EndsWith("mp4");
                                    if (fName.StartsWith(clientProps.Prefix) && (_mp3 || _mp4))
                                    {
                                        result.Append(fName);
                                        result.Append("\n");
                                    }
                                }
                            }
                        }
                    }
                }
                reader.Close();
            }
            catch
            {
                result = new StringBuilder();
                reader.Close();
            }
            if (result.Length > 0)
            {
                result.Remove(result.ToString().LastIndexOf("\n"), 1);
                return result.ToString().Split('\n');
            }

            return null;
        }

        private void createFolders()
        {
            setFoldersName();
            string directoryString = clientProps.CurrDirectory + "\\" + clientProps.TodayFolderName;
            if (!Directory.Exists(directoryString))
                System.IO.Directory.CreateDirectory(directoryString);
            directoryString = clientProps.CurrDirectory + "\\" + clientProps.YesterdayFolderName;
            if (!Directory.Exists(directoryString))
                System.IO.Directory.CreateDirectory(directoryString);
        }

        private void SetUserProperies()
        {
            Settings.Default.audiomp3 = clientProps.Audiomp3;
            Settings.Default.videomp4 = clientProps.Videomp4;
            Settings.Default.video = clientProps.Video;
            Settings.Default.size = clientProps.Size;
            Settings.Default.currDirectory = clientProps.CurrDirectory;
            Settings.Default.language = clientProps.Language;
            Settings.Default.copyToRemovable = clientProps.CopyToRemovable;
            Settings.Default.removeOldFolders = clientProps.RemoveOldFolders;
            Settings.Default.Save();
        }

        private void ClearUserProperies()
        {
            Settings.Default.audiomp3 = true;
            Settings.Default.videomp4 = false;
            Settings.Default.video = false;
            Settings.Default.size = "Normal";
            Settings.Default.currDirectory = "";
            Settings.Default.language = "English";
            Settings.Default.copyToRemovable = false;
            Settings.Default.removeOldFolders = false;
            Settings.Default.Save();
        }

        private void deleteOldDirectories()
        {
            System.IO.DirectoryInfo[] subDirs = null;
            StringBuilder deleteFolderList = new StringBuilder();

            DateTime Today = new DateTime();
            Today = DateTime.ParseExact((System.DateTime.Now).ToString(FDConstants.dateFormat), FDConstants.dateFormat, null);

            DirectoryInfo dInfo = new DirectoryInfo(clientProps.CurrDirectory);

            if (dInfo.Exists)
            {
                subDirs = dInfo.GetDirectories();
                DirectoryInfo di;
                foreach (DirectoryInfo dir in subDirs)
                {
                    DateTime dateTime;
                    if (DateTime.TryParse(dir.ToString(), out dateTime))
                        if ((Today - dateTime).Days > 14)
                        {
                            di = new DirectoryInfo(dir.FullName);
                            di.Delete(true);
                        }
                }
            }
        }

        private void lblStatusTreadsHandler(System.Windows.Controls.Label lbl, string text)
        {
            if (!lbl.Dispatcher.CheckAccess())
            {
                lbl.Dispatcher.BeginInvoke(System.Windows.Threading.DispatcherPriority.Normal,
                  new Action(
                    delegate()
                    {
                        lbl.Content = text;
                    }
                ));
            }
            else
            {
                lbl.Content = text;
            }
        }
        #endregion

        #region Download Functions
        private void startDownloadProcess()
        {
            var newEventArgs = new RoutedEventArgs(System.Windows.Controls.Button.ClickEvent);
            if (!btnStart.Dispatcher.CheckAccess())
            {
                btnStart.Dispatcher.BeginInvoke(
                  System.Windows.Threading.DispatcherPriority.Normal,
                  new Action(
                    delegate()
                    {
                        btnStart.RaiseEvent(newEventArgs);
                    }
                ));
            }
            else
            {
                btnStart.RaiseEvent(newEventArgs);
            }
        }

        private void StartDownloadToRemovable()
        {
            if (NetworkStatus.IsAvailable)
            {
                downloader.Files.Clear();
                pBarFileProgress.Value = 0;
                pBarTotalProgress.Value = 0;
                lblFileProgress.Content = "";
                lblTotalProgress.Content = "";
                lblStatus.Content = "";
                downloader.LocalDirectory = clientProps.RemovableDeviceDirectory;

                string[] remoteFiles = getFilesListFromXML(true);
                string[] localFiles = GetLocalFilesList(true);
                string[] filesToDownload = GetRemoteLocalFilesDiff(remoteFiles, localFiles);
                if (filesToDownload == null)
                {
                    lblStatus.Content = "No new Lessons Found";
                    return;
                }

                if (webBrowserFrm == null)
                {
                    webBrowserFrm = new WebBrowserForm();
                    webBrowserFrm.WebBrowserForm_Load(new object[] { null }, new EventArgs { });
                }

                m_removableDownloadInProcess = true;
                lblRemovable.Content = "Downloading to: " + clientProps.RemovableDeviceDirectory;
                // Get the contents of the rich text box
                foreach (string fl in filesToDownload)
                {
                    // Note: check if the url is valid before adding it, and probably should do this is a real application
                    downloader.Files.Add(new FileDownloader.FileInfo(FDConstants.ftpServerPath + fl));
                }
                // Start the downloader
                downloader.Start();
            }
            else
            {
                lblStatusTreadsHandler(lblStatus, FDConstants.connProblemsMsg);
            }
        }
        #endregion

    }
}