using System;
using System.Collections.Generic;
using System.Linq;
using System.Windows.Forms;
using System.Diagnostics;

namespace UninstallApp
{
    static class Program
    {
        /// <summary>
        /// The main entry point for the application.
        /// </summary>
        [STAThread]
        static void Main()
        {
            string[] arguments = Environment.GetCommandLineArgs();

            foreach (string argument in arguments)
            {
                string[] parameters = argument.Split('=');
                if (parameters[0].ToLower() == "/u")
                {
                    string productCode = parameters[1];
                    string path = Environment.GetFolderPath(Environment.SpecialFolder.System);
                    Process proc = new Process();
                    proc.StartInfo.FileName = string.Concat(path, "\\msiexec.exe");
                    proc.StartInfo.Arguments = string.Concat(" /x ", productCode);
                    proc.Start();
                    Application.Exit();
                }
            }
       }
    }
}
