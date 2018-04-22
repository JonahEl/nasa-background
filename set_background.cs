using System;
using System.Data;
using System.Text;
using System.Runtime.InteropServices;

/*
to build run: [PATH_TO_dotNET_FRAMEWORK]\csc /out:set_background.exe .\set_background.cs
*/
namespace cwp
{
   class Program
    {
       [DllImport("user32.dll")]
       public static extern Int32 SystemParametersInfo(
           UInt32 action, UInt32 uParam, String vParam, UInt32 winIni);

        public static readonly UInt32 SPI_SETDESKWALLPAPER  = 0x14;
        public static readonly UInt32 SPIF_UPDATEINIFILE    = 0x01;
        public static readonly UInt32 SPIF_SENDWININICHANGE = 0x02;

        public static void SetWallpaper(String path)
        {
            Console.WriteLine("Setting wallpaper to '" + path + "'");
            SystemParametersInfo(SPI_SETDESKWALLPAPER, 0, path,
                SPIF_UPDATEINIFILE | SPIF_SENDWININICHANGE);
        }

        public static void Main(string[] args)
        {
            if (args.Length >= 1)
            {
                SetWallpaper( args[0] );
            }
        }
    }
}