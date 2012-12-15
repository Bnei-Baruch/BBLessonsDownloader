using System;
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
using System.Net.Mail;
using System.Net;

namespace BBLessonsDwnldApp
{
    /// <summary>
    /// Interaction logic for SendMail.xaml
    /// </summary>
    public partial class SendMail : Window
    {
        public SendMail()
        {
            InitializeComponent();
        }

        private void Btn_SendMail_Click(object sender, RoutedEventArgs e)
        {
            try
            {
                MailMessage mM = new MailMessage();
                mM.From = new MailAddress("bneibaruchdailydownloaderhelp@yahoo.com");
                mM.To.Add("archive@kabbalahmedia.info");
                mM.Subject = textSubject.Text;
                mM.Body = "From: " + textFrom.Text + " " + "\n" + TextBody.Text;
                mM.IsBodyHtml = true;
                mM.Priority = MailPriority.High;
                SmtpClient sC = new SmtpClient("smtp.mail.yahoo.com");
                sC.Port = 587;
                sC.Credentials = new NetworkCredential("bneibaruchdailydownloaderhelp", "murtan77");
                sC.Send(mM);
                MessageBox.Show("mail sent successfully");
                this.Close();
            }
            catch (InvalidOperationException err)
            {
                MessageBox.Show("Error: " + err.ToString());//, MessageBoxButtons.OK);
                this.Close();
                
            }
        }
    }
}
