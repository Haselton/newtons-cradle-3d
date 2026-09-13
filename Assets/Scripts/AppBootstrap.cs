using UnityEngine;

public class AppBootstrap : MonoBehaviour
{
    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
    static void Boot()
    {
        if (FindObjectOfType<CradleController>() != null) return;
        var root = new GameObject("NewtonCradleApp");
        root.AddComponent<CradleController>();
        root.AddComponent<TouchInteractor>();
        root.AddComponent<CradleCamera>();
        root.AddComponent<CradleUI>();
        root.AddComponent<AdManager>();
    }
}
