using System.Collections.Generic;
using UnityEngine;

public class CradleController : MonoBehaviour
{
    public static CradleController I { get; private set; }
    public readonly List<CradleBall> balls = new();
    public Transform worldRoot;
    public Material chromeMaterial, frameMaterial, woodMaterial;
    public bool autoMode;
    public float timeScale = 1f;

    const int BallCount = 5;
    const float Radius = 0.29f;
    const float StringLength = 2.55f;
    const float Gap = 0.0025f;
    readonly Vector3 pivotBase = new Vector3(0, 2.65f, 0);

    void Awake()
    {
        I = this;
        Application.targetFrameRate = 60;
        QualitySettings.vSyncCount = 0;
        Physics.defaultSolverIterations = 16;
        Physics.defaultSolverVelocityIterations = 12;
        Physics.sleepThreshold = 0.001f;
        BuildWorld();
        BuildCradle();
    }

    void Update()
    {
        Time.timeScale = timeScale;
        if (autoMode && balls.Count > 0 && AllNearlyStill())
        {
            var b = balls[0];
            b.SetAngle(-34f);
            b.ReleaseFromDrag();
        }
    }

    bool AllNearlyStill()
    {
        foreach (var b in balls) if (b.Body.velocity.sqrMagnitude > 0.0025f) return false;
        return true;
    }

    Material Mat(string name, Color c, float metallic, float smooth)
    {
        var m = new Material(Shader.Find("Standard"));
        m.name = name; m.color = c; m.SetFloat("_Metallic", metallic); m.SetFloat("_Glossiness", smooth);
        return m;
    }

    void BuildWorld()
    {
        worldRoot = new GameObject("World").transform;
        chromeMaterial = Mat("Polished Chrome", new Color(.72f,.76f,.8f), 1f, .98f);
        frameMaterial = Mat("Graphite Frame", new Color(.08f,.09f,.105f), .85f, .72f);
        woodMaterial = Mat("Walnut", new Color(.22f,.095f,.035f), .15f, .48f);

        var floor = GameObject.CreatePrimitive(PrimitiveType.Cube);
        floor.name = "Walnut Desk"; floor.transform.SetParent(worldRoot);
        floor.transform.position = new Vector3(0,-.52f,0); floor.transform.localScale = new Vector3(8,.3f,6);
        floor.GetComponent<Renderer>().material = woodMaterial;

        RenderSettings.ambientMode = UnityEngine.Rendering.AmbientMode.Flat;
        RenderSettings.ambientLight = new Color(.16f,.17f,.2f);
        RenderSettings.fog = true; RenderSettings.fogColor = new Color(.035f,.04f,.055f); RenderSettings.fogDensity = .018f;

        MakeLight("Key", new Vector3(-3,5,-3), 2.2f, 7, new Color(1f,.86f,.72f));
        MakeLight("Fill", new Vector3(3,3,-1), 1.3f, 6, new Color(.62f,.75f,1f));
        MakeLight("Rim", new Vector3(0,4,3), 1.7f, 6, new Color(.7f,.85f,1f));
    }

    void MakeLight(string n, Vector3 p, float intensity, float range, Color color)
    {
        var g = new GameObject(n); g.transform.position = p; g.transform.SetParent(worldRoot);
        var l = g.AddComponent<Light>(); l.type = LightType.Point; l.intensity = intensity; l.range = range; l.color = color;
        l.shadows = LightShadows.Soft;
    }

    GameObject Beam(string n, Vector3 pos, Vector3 scale)
    {
        var g = GameObject.CreatePrimitive(PrimitiveType.Cube); g.name = n; g.transform.SetParent(worldRoot); g.transform.position = pos; g.transform.localScale = scale;
        g.GetComponent<Renderer>().material = frameMaterial; return g;
    }

    void BuildCradle()
    {
        Beam("BaseFront", new Vector3(0,-.17f,-.65f), new Vector3(4.0f,.18f,.18f));
        Beam("BaseBack",  new Vector3(0,-.17f,.65f),  new Vector3(4.0f,.18f,.18f));
        Beam("LeftPost",  new Vector3(-1.95f,1.35f,0), new Vector3(.16f,3.0f,.16f));
        Beam("RightPost", new Vector3(1.95f,1.35f,0), new Vector3(.16f,3.0f,.16f));
        Beam("TopFront",  new Vector3(0,2.78f,-.65f), new Vector3(4.05f,.14f,.14f));
        Beam("TopBack",   new Vector3(0,2.78f,.65f),  new Vector3(4.05f,.14f,.14f));

        float spacing = Radius*2f + Gap;
        float start = -spacing * (BallCount-1)/2f;
        for(int i=0;i<BallCount;i++)
        {
            float x = start + i*spacing;
            var pivot = new GameObject($"Pivot_{i}").transform; pivot.SetParent(worldRoot); pivot.position = pivotBase + new Vector3(x,0,0);
            var ball = GameObject.CreatePrimitive(PrimitiveType.Sphere); ball.name=$"Ball_{i}"; ball.transform.SetParent(worldRoot);
            ball.transform.localScale = Vector3.one * Radius*2f; ball.transform.position = pivot.position + Vector3.down*StringLength;
            ball.GetComponent<Renderer>().material = chromeMaterial;
            var rb=ball.AddComponent<Rigidbody>(); rb.mass=.52f; rb.drag=.002f; rb.angularDrag=.01f; rb.collisionDetectionMode=CollisionDetectionMode.ContinuousDynamic; rb.interpolation=RigidbodyInterpolation.Interpolate;
            var cc = ball.AddComponent<CradleBall>(); cc.index=i; cc.pivot=pivot; cc.length=StringLength; cc.radius=Radius; balls.Add(cc);
            cc.SetupConstraint();
        }
    }

    public void ResetCradle()
    {
        autoMode=false;
        foreach(var b in balls) b.ResetBall();
    }
    public void ToggleSlow() => timeScale = timeScale < .99f ? 1f : .35f;
    public void ToggleAuto() { autoMode = !autoMode; if(autoMode) ResetThenAuto(); }
    void ResetThenAuto(){ foreach(var b in balls) b.ResetBall(); autoMode=true; }
}
