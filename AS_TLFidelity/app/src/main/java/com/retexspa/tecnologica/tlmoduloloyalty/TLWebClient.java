package com.retexspa.tecnologica.tlmoduloloyalty;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

class TLWebClient extends WebViewClient {

    private Context context;
    private String cliente;

    TLWebClient(Context context) {
        this.context=context;
    }

    void setCliente(String cliente) {
        this.cliente = cliente;
    }
    public interface connection {
        firmaView getFirma(int id);
    }
    connection activityInterface;

    @Override
    public WebResourceResponse shouldInterceptRequest (WebView view, WebResourceRequest request) {
        Uri url = request.getUrl();
        if(url.getScheme()==null || url.getScheme().compareTo("tl")!=0)
            return super.shouldInterceptRequest(view, request);
        String path = url.getPath();
        if (path != null) {
            if (path.compareTo("/cliente") == 0) return getCliente();
            if (path.compareTo("/citta") == 0) return GetCitta(url);
            if (path.compareTo("/infoCitta") == 0) return GetInfoCitta(url);
            if (path.compareTo("/provincie") == 0) return GetProvincie(url);
            if (path.compareTo("/cap") == 0) return GetCAP(url);
            if (path.compareTo("/carte") == 0) return GetCarte(url);
            if (path.compareTo("/infoPV") == 0) return GetInfoPV();
            if (path.compareTo("/firma1.png") == 0) return GetFirma(1);
            if (path.compareTo("/firma2.png") == 0) return GetFirma(2);
        }
        WebResourceResponse ret =new WebResourceResponse("text","utf8", null);
        ret.setStatusCodeAndReasonPhrase(404,"not found");
        return ret;
    }

    private WebResourceResponse GetCarte(Uri url) {
        String startValue = url.getQueryParameter("term");
        String senior = url.getQueryParameter("senior");
        String emesse = url.getQueryParameter("emesse");
        TLFidelityWS web = new TLFidelityWS(context);
        String carte = web.getTessereSync(senior,emesse,startValue,10);
        byte[] info = carte.getBytes(StandardCharsets.UTF_8);
        return new WebResourceResponse("application/json","utf8", new ByteArrayInputStream(info));
    }

    private WebResourceResponse GetInfoPV() {
        String pvInfo = TLFidelityWS.getInfoPV();
        byte[] info = pvInfo.getBytes(StandardCharsets.UTF_8);
        return new WebResourceResponse("application/json","utf8", new ByteArrayInputStream(info));
    }

    private WebResourceResponse GetCitta(Uri url) {
        String startValue = url.getQueryParameter("term");
        String id = url.getQueryParameter("id");
        String jsonStr = "[]";
        if(startValue!=null) {
            LocalDB db = new LocalDB(context);
            String[] comuni = db.getComuni(startValue, id!=null && id.equals("cittaNascita"));
            try {
                jsonStr = (new JSONArray(comuni)).toString();
            } catch (JSONException e) {
                e.printStackTrace();
                jsonStr = "[]";
            }
        }
        byte[] info = jsonStr.getBytes(StandardCharsets.UTF_8);
        return new WebResourceResponse("application/json","utf8", new ByteArrayInputStream(info));
    }

    private WebResourceResponse GetProvincie(Uri url) {
        String citta = url.getQueryParameter("citta");
        String startValue = url.getQueryParameter("term");
        String jsonStr = "[]";
        if(citta!=null) {
            LocalDB db = new LocalDB(context);
            String[] provincie = db.getProvincie(citta, startValue != null ? startValue : "");
            try {
                jsonStr = (new JSONArray(provincie)).toString();
            } catch (JSONException e) {
                e.printStackTrace();
                jsonStr = "[]";
            }
        }
        byte[] info = jsonStr.getBytes(StandardCharsets.UTF_8);
        return new WebResourceResponse("application/json","utf8", new ByteArrayInputStream(info));
    }

    private WebResourceResponse GetCAP(Uri url) {
        String citta = url.getQueryParameter("citta");
        String startValue = url.getQueryParameter("term");
        String jsonStr = "[]";
        if(citta!=null) {
            LocalDB db = new LocalDB(context);
            String[] cap = db.getCAP(citta, startValue != null ? startValue : "");
            try {
                jsonStr = (new JSONArray(cap)).toString();
            } catch (JSONException e) {
                e.printStackTrace();
                jsonStr = "[]";
            }
        }
        byte[] info = jsonStr.getBytes(StandardCharsets.UTF_8);
        return new WebResourceResponse("application/json","utf8", new ByteArrayInputStream(info));
    }

    private WebResourceResponse GetInfoCitta(Uri url) {
        String citta = url.getQueryParameter("citta");
        LocalDB db = new LocalDB(context);
        String jsonStr = "{\"cap\": [], \"prov\": [] }";
        if(citta!=null) {
            try {
                JSONObject r = new JSONObject();
                r.putOpt("cap", new JSONArray(db.getCAP(citta, "")));
                r.putOpt("prov", new JSONArray(db.getProvincie(citta, "")));
                jsonStr = r.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        byte[] info = jsonStr.getBytes(StandardCharsets.UTF_8);
        return new WebResourceResponse("application/json","utf8", new ByteArrayInputStream(info));
    }

    @SuppressLint("ResourceType")
    private WebResourceResponse GetFirma(int id) {
        try {
            final int width = 900;
            final int height = 300;

            firmaView firma=null;
            if(activityInterface!=null) firma=activityInterface.getFirma(id);
            if(firma!=null && firma.hasData()) {
                byte [] info = firma.getImage(width,height);
                return new WebResourceResponse("image/png","utf8", new ByteArrayInputStream(info));
            }
            return new WebResourceResponse("image/png","utf8", context.getResources().openRawResource(R.drawable.firma));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private WebResourceResponse getCliente() {
        byte[] info = cliente.getBytes(StandardCharsets.UTF_8);
        return new WebResourceResponse("application/json","utf8", new ByteArrayInputStream(info));
    }

}
