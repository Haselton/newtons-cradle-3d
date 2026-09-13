using UnityEngine;
using UnityEngine.EventSystems;

public class TouchInteractor : MonoBehaviour
{
    Camera cam; CradleBall active; Plane dragPlane;
    void Start(){ cam=Camera.main; }
    void Update()
    {
        if(cam==null) cam=Camera.main;
        if(Input.touchCount>0)
        {
            var t=Input.GetTouch(0);
            if(t.phase==TouchPhase.Began) Begin(t.position);
            else if((t.phase==TouchPhase.Moved||t.phase==TouchPhase.Stationary)&&active) Move(t.position);
            else if((t.phase==TouchPhase.Ended||t.phase==TouchPhase.Canceled)&&active) End();
        }
#if UNITY_EDITOR || UNITY_STANDALONE
        if(Input.GetMouseButtonDown(0)) Begin(Input.mousePosition);
        if(Input.GetMouseButton(0)&&active) Move(Input.mousePosition);
        if(Input.GetMouseButtonUp(0)&&active) End();
#endif
    }
    void Begin(Vector2 p)
    {
        if(EventSystem.current!=null && EventSystem.current.IsPointerOverGameObject()) return;
        var r=cam.ScreenPointToRay(p);
        if(Physics.Raycast(r,out var hit,100f))
        {
            active=hit.collider.GetComponent<CradleBall>(); if(active==null)return;
            active.BeginDrag(); dragPlane=new Plane(Vector3.forward,active.transform.position);
        }
    }
    void Move(Vector2 p)
    {
        var r=cam.ScreenPointToRay(p); if(dragPlane.Raycast(r,out float d)) active.DragToWorld(r.GetPoint(d));
    }
    void End(){ active.ReleaseFromDrag(); active=null; }
}
