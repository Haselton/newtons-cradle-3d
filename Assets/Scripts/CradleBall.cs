using UnityEngine;

public class CradleBall : MonoBehaviour
{
    public int index; public Transform pivot; public float length, radius;
    public Rigidbody Body { get; private set; }
    public bool dragging;
    Vector3 home;
    LineRenderer lineA, lineB;
    AudioSource audioSource;
    static AudioClip syntheticTick;

    public void SetupConstraint()
    {
        Body = GetComponent<Rigidbody>(); home = transform.position;
        var joint = gameObject.AddComponent<ConfigurableJoint>();
        joint.autoConfigureConnectedAnchor=false; joint.connectedBody=null; joint.anchor=Vector3.zero; joint.connectedAnchor=pivot.position;
        joint.xMotion=ConfigurableJointMotion.Limited; joint.yMotion=ConfigurableJointMotion.Limited; joint.zMotion=ConfigurableJointMotion.Limited;
        var lim=joint.linearLimit; lim.limit=length; lim.contactDistance=.001f; joint.linearLimit=lim;
        joint.angularXMotion=ConfigurableJointMotion.Free; joint.angularYMotion=ConfigurableJointMotion.Free; joint.angularZMotion=ConfigurableJointMotion.Free;
        joint.enableCollision=false;
        lineA=MakeLine("StringA"); lineB=MakeLine("StringB");
        audioSource=gameObject.AddComponent<AudioSource>(); audioSource.spatialBlend=.72f; audioSource.minDistance=.4f; audioSource.maxDistance=7f;
        if(syntheticTick==null) syntheticTick=GenerateTick();
    }

    LineRenderer MakeLine(string n)
    {
        var g=new GameObject(n); g.transform.SetParent(transform.parent); var lr=g.AddComponent<LineRenderer>();
        lr.positionCount=2; lr.startWidth=.012f; lr.endWidth=.012f; lr.material=new Material(Shader.Find("Sprites/Default"));
        lr.startColor=lr.endColor=new Color(.12f,.12f,.13f); return lr;
    }
    void LateUpdate()
    {
        float z=.28f;
        lineA.SetPosition(0,pivot.position+new Vector3(0,0,-z)); lineA.SetPosition(1,transform.position+new Vector3(0,.03f,-.11f));
        lineB.SetPosition(0,pivot.position+new Vector3(0,0,z)); lineB.SetPosition(1,transform.position+new Vector3(0,.03f,.11f));
    }

    void FixedUpdate()
    {
        if(dragging) return;
        var p=transform.position; p.z=Mathf.Lerp(p.z,pivot.position.z,.58f); transform.position=p;
        var v=Body.velocity; v.z*=.18f; Body.velocity=v;
    }

    void OnCollisionEnter(Collision c)
    {
        if(!c.collider.name.StartsWith("Ball_")) return;
        float impulse=c.impulse.magnitude;
        if(impulse<.025f) return;
        float volume=Mathf.Clamp01(.13f+impulse*.23f);
        audioSource.pitch=Random.Range(.965f,1.035f);
        audioSource.PlayOneShot(syntheticTick,volume);
#if UNITY_ANDROID && !UNITY_EDITOR
        if(volume>.18f) Handheld.Vibrate();
#endif
    }

    static AudioClip GenerateTick()
    {
        int rate=44100, count=(int)(rate*.12f); float[] s=new float[count];
        for(int i=0;i<count;i++)
        {
            float t=(float)i/rate; float env=Mathf.Exp(-38f*t);
            s[i]=(Mathf.Sin(2*Mathf.PI*2350*t)*.55f + Mathf.Sin(2*Mathf.PI*4050*t)*.28f + Random.Range(-.06f,.06f))*env;
        }
        var c=AudioClip.Create("SteelImpact",count,1,rate,false); c.SetData(s,0); return c;
    }

    public void BeginDrag(){ dragging=true; Body.isKinematic=true; }
    public void DragToWorld(Vector3 w)
    {
        Vector3 d=w-pivot.position; d.z=0; if(d.sqrMagnitude<.001f) return; d=d.normalized*length; float maxX=length*.86f; d.x=Mathf.Clamp(d.x,-maxX,maxX); d.y=-Mathf.Sqrt(Mathf.Max(.001f,length*length-d.x*d.x));
        transform.position=pivot.position+d;
    }
    public void ReleaseFromDrag(){ dragging=false; Body.isKinematic=false; Body.velocity=Vector3.zero; Body.angularVelocity=Vector3.zero; }
    public void SetAngle(float degrees)
    {
        float r=degrees*Mathf.Deg2Rad; transform.position=pivot.position+new Vector3(Mathf.Sin(r)*length,-Mathf.Cos(r)*length,0);
    }
    public void ResetBall(){ dragging=false; Body.isKinematic=false; transform.position=home; Body.velocity=Vector3.zero; Body.angularVelocity=Vector3.zero; }
}
