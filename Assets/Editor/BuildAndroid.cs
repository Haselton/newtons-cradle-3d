#if UNITY_EDITOR
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEngine;
using System.IO;

public static class BuildAndroid
{
    [MenuItem("Newton Cradle/Build Debug APK")]
    public static void BuildDebug()
    {
        EnsureScene();
        PlayerSettings.productName="Newton's Cradle 3D";
        PlayerSettings.companyName="Haselton Media Group";
        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android,"com.haseltonmediagroup.newtonscradle3d");
        PlayerSettings.Android.minSdkVersion=AndroidSdkVersions.AndroidApiLevel26;
        EditorUserBuildSettings.buildAppBundle=false;
        Directory.CreateDirectory("Builds");
        BuildPipeline.BuildPlayer(new[]{"Assets/Scenes/Main.unity"},"Builds/NewtonsCradle3D-debug.apk",BuildTarget.Android,BuildOptions.Development);
    }
    [MenuItem("Newton Cradle/Build Release AAB")]
    public static void BuildAab()
    {
        EnsureScene();
        PlayerSettings.productName="Newton's Cradle 3D"; PlayerSettings.companyName="Haselton Media Group";
        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android,"com.haseltonmediagroup.newtonscradle3d");
        EditorUserBuildSettings.buildAppBundle=true; Directory.CreateDirectory("Builds");
        BuildPipeline.BuildPlayer(new[]{"Assets/Scenes/Main.unity"},"Builds/NewtonsCradle3D.aab",BuildTarget.Android,BuildOptions.None);
    }
    static void EnsureScene()
    {
        if(File.Exists("Assets/Scenes/Main.unity")) return;
        var s=UnityEditor.SceneManagement.EditorSceneManager.NewScene(UnityEditor.SceneManagement.NewSceneSetup.EmptyScene,UnityEditor.SceneManagement.NewSceneMode.Single);
        UnityEditor.SceneManagement.EditorSceneManager.SaveScene(s,"Assets/Scenes/Main.unity");
    }
}
#endif
