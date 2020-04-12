using Microsoft.Build.Locator;
using System;
using System.Linq;
using System.Reflection;

namespace ProjectFileParser
{
    public static class MSBuildCustomLocator
    {
        public static void Register()
        {
            try
            {
                var latestVsVersion = MSBuildLocator.QueryVisualStudioInstances().OrderBy(vsInstance => vsInstance.Version).Last();
                MSBuildLocator.RegisterInstance(latestVsVersion);
                Console.Error.WriteLine($"Registered latest VS Instance: {latestVsVersion.Name} - {latestVsVersion.Version} - {latestVsVersion.MSBuildPath} - {latestVsVersion.DiscoveryType} - {latestVsVersion.VisualStudioRootPath}");
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine("MSBuildLocator cannot detect VS location, falling back to GAC registered MSBuild dlls");
                Console.Error.WriteLine("Error was: {0}", ex);

                RegisterFallback();
            }
        }

        private static void RegisterFallback()
        {
            AppDomain.CurrentDomain.AssemblyResolve += (object sender, ResolveEventArgs args) =>
            {
                return Assembly.LoadFrom("privateDlls");
            };
        }
    }
}