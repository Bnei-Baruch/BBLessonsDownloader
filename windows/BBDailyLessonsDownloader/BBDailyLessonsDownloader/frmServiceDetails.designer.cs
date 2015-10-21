namespace ServiceInstaller
{
    partial class frmServiceDetails
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(frmServiceDetails));
            this.instLanguage = new System.Windows.Forms.Label();
            this.textLessonsDirectory = new System.Windows.Forms.TextBox();
            this.btnSaveSettings = new System.Windows.Forms.Button();
            this.comboInstLang = new System.Windows.Forms.ComboBox();
            this.lblLessonsDirectory = new System.Windows.Forms.Label();
            this.btnBrowse = new System.Windows.Forms.Button();
            this.SuspendLayout();
            // 
            // instLanguage
            // 
            this.instLanguage.AutoSize = true;
            this.instLanguage.Location = new System.Drawing.Point(12, 39);
            this.instLanguage.Name = "instLanguage";
            this.instLanguage.Size = new System.Drawing.Size(58, 13);
            this.instLanguage.TabIndex = 0;
            this.instLanguage.Text = "Language:";
            // 
            // textLessonsDirectory
            // 
            this.textLessonsDirectory.Location = new System.Drawing.Point(112, 69);
            this.textLessonsDirectory.Name = "textLessonsDirectory";
            this.textLessonsDirectory.Size = new System.Drawing.Size(310, 20);
            this.textLessonsDirectory.TabIndex = 1;
            // 
            // btnSaveSettings
            // 
            this.btnSaveSettings.DialogResult = System.Windows.Forms.DialogResult.OK;
            this.btnSaveSettings.Location = new System.Drawing.Point(422, 185);
            this.btnSaveSettings.Name = "btnSaveSettings";
            this.btnSaveSettings.Size = new System.Drawing.Size(76, 26);
            this.btnSaveSettings.TabIndex = 2;
            this.btnSaveSettings.Text = "OK";
            this.btnSaveSettings.UseVisualStyleBackColor = true;
            this.btnSaveSettings.Click += new System.EventHandler(this.btnSaveSettings_Click);
            // 
            // comboInstLang
            // 
            this.comboInstLang.FormattingEnabled = true;
            this.comboInstLang.Location = new System.Drawing.Point(112, 36);
            this.comboInstLang.Name = "comboInstLang";
            this.comboInstLang.Size = new System.Drawing.Size(121, 21);
            this.comboInstLang.TabIndex = 4;
            this.comboInstLang.SelectedIndexChanged += new System.EventHandler(this.comboInstLang_SelectedIndexChanged);
            // 
            // lblLessonsDirectory
            // 
            this.lblLessonsDirectory.AutoSize = true;
            this.lblLessonsDirectory.Location = new System.Drawing.Point(12, 73);
            this.lblLessonsDirectory.Name = "lblLessonsDirectory";
            this.lblLessonsDirectory.Size = new System.Drawing.Size(94, 13);
            this.lblLessonsDirectory.TabIndex = 5;
            this.lblLessonsDirectory.Text = "Lessons Directory:";
            // 
            // btnBrowse
            // 
            this.btnBrowse.Location = new System.Drawing.Point(423, 68);
            this.btnBrowse.Name = "btnBrowse";
            this.btnBrowse.Size = new System.Drawing.Size(75, 23);
            this.btnBrowse.TabIndex = 6;
            this.btnBrowse.Text = "Browse";
            this.btnBrowse.UseVisualStyleBackColor = true;
            this.btnBrowse.Click += new System.EventHandler(this.btnBrowse_Click);
            // 
            // frmServiceDetails
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(510, 223);
            this.ControlBox = false;
            this.Controls.Add(this.btnBrowse);
            this.Controls.Add(this.lblLessonsDirectory);
            this.Controls.Add(this.comboInstLang);
            this.Controls.Add(this.btnSaveSettings);
            this.Controls.Add(this.textLessonsDirectory);
            this.Controls.Add(this.instLanguage);
            this.Icon = ((System.Drawing.Icon)(resources.GetObject("$this.Icon")));
            this.Name = "frmServiceDetails";
            this.StartPosition = System.Windows.Forms.FormStartPosition.CenterScreen;
            this.Text = "Default Application Settings ";
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.Label instLanguage;
        private System.Windows.Forms.TextBox textLessonsDirectory;
        private System.Windows.Forms.Button btnSaveSettings;
        private System.Windows.Forms.ComboBox comboInstLang;
        private System.Windows.Forms.Label lblLessonsDirectory;
        private System.Windows.Forms.Button btnBrowse;
    }
}