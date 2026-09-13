using UnityEngine;
using UnityEngine.UI;
using UnityEngine.EventSystems;

public class CradleUI : MonoBehaviour
{
    Font font;
    void Start()
    {
        font=Resources.GetBuiltinResource<Font>("Arial.ttf");
        if(FindObjectOfType<EventSystem>()==null){ var e=new GameObject("EventSystem"); e.AddComponent<EventSystem>(); e.AddComponent<StandaloneInputModule>(); }
        var canvasGO=new GameObject("UI"); var c=canvasGO.AddComponent<Canvas>(); c.renderMode=RenderMode.ScreenSpaceOverlay; canvasGO.AddComponent<CanvasScaler>().uiScaleMode=CanvasScaler.ScaleMode.ScaleWithScreenSize; canvasGO.AddComponent<GraphicRaycaster>();
        Title(c.transform);
        Button(c.transform,"RESET",new Vector2(-270,70),()=>CradleController.I.ResetCradle());
        Button(c.transform,"SLOW",new Vector2(-90,70),()=>CradleController.I.ToggleSlow());
        Button(c.transform,"AUTO",new Vector2(90,70),()=>CradleController.I.ToggleAuto());
        Button(c.transform,"CAMERA",new Vector2(270,70),()=>FindObjectOfType<CradleCamera>().ResetView());
        var hint=Text(c.transform,"Drag a steel ball • two fingers orbit/zoom",18,TextAnchor.MiddleCenter); SetRect(hint.rectTransform,new Vector2(0,122),new Vector2(700,34));
    }
    void Title(Transform p){ var t=Text(p,"NEWTON'S CRADLE",28,TextAnchor.MiddleCenter); t.fontStyle=FontStyle.Bold; SetRect(t.rectTransform,new Vector2(0,-55),new Vector2(700,52),true); }
    Text Text(Transform p,string s,int size,TextAnchor a){var g=new GameObject(s);g.transform.SetParent(p,false);var t=g.AddComponent<Text>();t.text=s;t.font=font;t.fontSize=size;t.alignment=a;t.color=new Color(1,1,1,.92f);return t;}
    void Button(Transform p,string s,Vector2 pos,UnityEngine.Events.UnityAction act){var g=new GameObject(s);g.transform.SetParent(p,false);var img=g.AddComponent<Image>();img.color=new Color(.06f,.07f,.085f,.78f);var b=g.AddComponent<Button>();b.onClick.AddListener(act);var r=g.GetComponent<RectTransform>();SetRect(r,pos,new Vector2(160,58));var t=Text(g.transform,s,18,TextAnchor.MiddleCenter);SetRect(t.rectTransform,Vector2.zero,new Vector2(160,58));}
    void SetRect(RectTransform r,Vector2 pos,Vector2 size,bool top=false){r.anchorMin=r.anchorMax=top?new Vector2(.5f,1):new Vector2(.5f,0);r.pivot=new Vector2(.5f,.5f);r.anchoredPosition=pos;r.sizeDelta=size;}
}
