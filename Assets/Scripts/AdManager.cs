using UnityEngine;

// Production AdMob adapter.
// The app compiles and runs without the Google Mobile Ads package.
// After importing Google's Unity plugin, add ADMOB_PRESENT to Scripting Define Symbols
// and replace the placeholder IDs below with the app's production IDs.
public class AdManager : MonoBehaviour
{
    [Header("Replace before Play release")]
    public string androidAppId = "ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY";
    public string bannerId = "ca-app-pub-XXXXXXXXXXXXXXXX/BBBBBBBBBB";
    public string interstitialId = "ca-app-pub-XXXXXXXXXXXXXXXX/IIIIIIIIII";
    int resets;

    void Start()
    {
#if ADMOB_PRESENT
        GoogleMobileAds.Api.MobileAds.Initialize(_ => LoadBanner());
#endif
    }

    public void RegisterNaturalBreak()
    {
        resets++;
        if(resets % 6 == 0) ShowInterstitial();
    }

#if ADMOB_PRESENT
    GoogleMobileAds.Api.BannerView banner;
    GoogleMobileAds.Api.InterstitialAd interstitial;
    void LoadBanner()
    {
        banner = new GoogleMobileAds.Api.BannerView(bannerId, GoogleMobileAds.Api.AdSize.GetCurrentOrientationAnchoredAdaptiveBannerAdSizeWithWidth(GoogleMobileAds.Api.AdSize.FullWidth), GoogleMobileAds.Api.AdPosition.Bottom);
        banner.LoadAd(new GoogleMobileAds.Api.AdRequest());
        LoadInterstitial();
    }
    void LoadInterstitial()
    {
        GoogleMobileAds.Api.InterstitialAd.Load(interstitialId,new GoogleMobileAds.Api.AdRequest(),(ad,err)=>{if(err==null) interstitial=ad;});
    }
    void ShowInterstitial(){ if(interstitial!=null && interstitial.CanShowAd()){ interstitial.Show(); interstitial=null; LoadInterstitial(); } }
#else
    void ShowInterstitial() { }
#endif
}
