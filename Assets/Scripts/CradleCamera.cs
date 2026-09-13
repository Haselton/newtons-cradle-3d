using UnityEngine;

public class CradleCamera : MonoBehaviour
{
    Camera cam; Vector3 target=new Vector3(0,1.15f,0); float yaw, pitch=8f, dist=6.8f;
    void Start()
    {
        var g=new GameObject("Main Camera"); cam=g.AddComponent<Camera>(); g.tag="MainCamera"; cam.fieldOfView=42f; cam.allowHDR=true;
        g.AddComponent<AudioListener>(); Position();
    }
    void LateUpdate()
    {
        if(Input.touchCount==2)
        {
            var a=Input.GetTouch(0); var b=Input.GetTouch(1);
            Vector2 da=a.deltaPosition, db=b.deltaPosition;
            Vector2 avg=(da+db)*.5f; yaw+=avg.x*.08f; pitch-=avg.y*.06f; pitch=Mathf.Clamp(pitch,-8,32);
            float prev=(a.position-a.deltaPosition-(b.position-b.deltaPosition)).magnitude; float now=(a.position-b.position).magnitude; dist=Mathf.Clamp(dist-(now-prev)*.007f,4.8f,8.8f);
            Position();
        }
    }
    void Position()
    {
        if(cam==null)return; Quaternion q=Quaternion.Euler(pitch,yaw,0); cam.transform.position=target+q*new Vector3(0,0,-dist); cam.transform.LookAt(target);
    }
    public void ResetView(){ yaw=0; pitch=8; dist=6.8f; Position(); }
}
