const info_open = $("#info_open")
  , info_main = $("#info")
  , info_close = $("#info_close")
  , info_refresh = $("#info_refresh")
  , mask = $("#mask")
  , loading = $("#loading")
  , copy = $("#link")
  , snack = $("#snackbar")
  , dialogue = $("#dialogue")
  , dia_close = $("#d_close")
  , dia_okay = $("#d_okay")
  , dia_icon = $("#d_icon")
  , dia_title = $("#d_title")
  , dia_subtitle = $("#d_subtitle")
  , portal_close = $("#portal_close")
  , main = $("#main")
  , info_ma = $("#i_max_s")
  , info_bw = $("#i_to_band")
  , info_tp = $("#total_packs")
  , info_net = $("#net_name")
  , zip = new JSZip
  , info_ls = $("#link_speed");
    window.HOLD_LOAD = !0
  , window.TOTAL_BYTES = 0
  , window.LOAD_DELAY_DONE = !1;
MAX_SPEED = "Speed: 0BPS",
TOTAL_BAND = "Total bandwidth: 0B",
TOTAL_PACKS = "Total packages: 0",
NET_NAME = "Network: Detecting...",
DATA_SETS = {},
LINK_SPEED = "Link speed: 0MBPS";
window.bucketChecksum = null;
const info_up = e=>{
    e && "object" == typeof e && (e.linkSpeed && (LINK_SPEED = "Link speed: " + e.link_speed),
    e.ssid && (NET_NAME = "Network: " + e.ssid));
    window.bucketChecksum = e.bucketChecksum;
}
;
let err = 0;
const info_spin = p=>{
    err >= 10 && (snack_show("Sync error!"),
    err = 0),
    $.post("/info", {
        p: p
    }).done(d=>{
        if(d === 'WRONG-PIN'){
            snack_show("PIN was changed! Exitting...");
            setTimeout(() => {
                window.location.href = "/bucket_error.html";
            });
            return;
        }
        else {
            try {
                let dx = JSON.parse(d);
                if(window.bucketChecksum !== dx.bucketChecksum){
                    $.post("/bucket", { p: p })
                    .done(response => {
                        if (response !== null && response !== "" && response !== "WRONG-PIN") {
                            bucket_data_load(response, p);
                        }
                    })
                    .fail(() => {
                        window.location.href = "/bucket_error.html";
                    });
                }
                eval("info_up(" + d + ")");
                err = 0;
            } catch (e) {
                err++
            }
            setTimeout(()=>{
                info_spin(p)
            }
            , 1e3);
        }
    }
    ).fail(()=>{
        err++,
        setTimeout(()=>{
            info_spin(p)
        }
        , 1e3)
    }
    )
};

let mainLink = window.location.href;

if (mainLink) {
    let extractedCode = mainLink.substr(mainLink.indexOf("x-") + 2, 5);
    if (extractedCode && extractedCode.length === 5) {
        $("#link").text("iOS/PC link: " + location.protocol + "//" + location.host);
        $("#pin").text("Pin(iOS/PC): " + extractedCode);
        
        info_spin(extractedCode);
    }
}

window.CURRENT_FILE_INDEX = 0;
window.size_set = {};
window.hash_set = {};

const bucket_data_load = (m, i)=> {
    LOAD_DELAY_DONE ? load_hide() : HOLD_LOAD = !1;
    try{
        m = JSON.parse(m);
        let name_set = [];
        let x = 0;
        for(x; x < m.length; x++){
            name_set[x] = m[x].name;
            window.size_set[name_set[x]] = m[x].size;
            window.hash_set[name_set[x]] = m[x].hash;
        }
        laod_bucket_links(name_set, i);
        CURRENT_FILE_INDEX = x;
    }
    catch(e){
        console.log(e);
        //window.location.href = "/bucket_error.html"
    }
};

f_size = e=>{
    let i = e + "B";
    return e > 1e3 && (i = (e = Math.round(e / 1e3)) + "KB"),
    e > 1e3 && (i = (e = Math.round(e / 1e3)) + "MB"),
    e > 1e3 && (i = (e = Math.round(e / 1e3)) + "GB"),
    i
}
  , get_file_name = e=>e.substr(e.lastIndexOf("/") + 1)
  , no_prev = ()=>{
    snack_show("Preview is not available!")
}
  , make_grid = (e,t)=>{
    
    if (!t || 0 == t.length)
        return;

    let a = ""
      , s = '<div class="cf gb" onclick="snack_show(\'No more files in this catagory\')">See all</div>'
      , l = "fa-file";
    for ("Images" == e && (l = "fa-image"),
    "Audio" == e && (l = "fa-music"),
    t.length > 8 && (window.sub_array_temp = t,
    s = '<div class="cf ab" onclick="open_sub(window.sub_array_temp, \'' + e + "', '" + l + "')\">See all</div>"),
    i = 0; i < t.length && 8 != i; i++) {
        let e = t[i]
          , s = "Unknown file"
          , n = get_file_name(e);
        n && (s = n),
        a += '<div class="file_child" onclick="no_prev()"><div class="cf"><i class="fa ' + l + '"></i></div><div class="ti">' + s + "</div></div>"
    }
    let n = '<section><div class="fg_t"><div><i class="fa ' + l + '"></i> ' + e + "(" + t.length + ")</div>" + s + '</div><div class="files_grid">' + a + "</div></section>";
    $("#files_grid").append(n)
}
  , open_sub = (e,i,t)=>{
    if (e && 0 != e.length) {
        $(".na").text(i);
        let z = i;
        for (x = 0; x < e.length; x++){
            let i = e[x]
              , a = get_extension(i)
              , s = '<div class="list_child" onclick="no_prev()"><div class="li cf"><i class="fa ' + t + '"></i></div><div class="lt"><div class="ln">' + get_file_name(i) + '</div><div class="in">' + a.toLocaleUpperCase() + " file</div></div></div>";
            $(".sub_body").append(s);
            $(".na").text(z + "(" + (x+1) + ")")
        }
        $("#main").css("display", "none"),
        $("#sub").css("display", "block")
    }
}
  , sub_close = ()=>{
    $("#main").css("display", ""),
    $("#sub").css("display", "none"),
    $(".sub_body").html("")
}
  , snack_show = (e,d=1)=>{
    window.st && clearTimeout(window.st),
    snack.text(e),
    snack.addClass("show");
    if(d){
    window.st = setTimeout(()=>{
        snack.removeClass("show"),
        window.st = !1
    }
    , 2900)
}
}
  , load_hide = ()=>{
    loading.css("margin-left", "-100%"),
    main.css("opacity", "1"),
    setTimeout(()=>{
        loading.remove()
    }
    , 300)
}
  , info_build = ()=>{
    info_ma.text(MAX_SPEED),
    info_bw.text("Total bandwidth: " + f_size(window.TOTAL_BYTES)),
    info_tp.text(TOTAL_PACKS),
    info_net.text(NET_NAME),
    info_ls.text(LINK_SPEED),
    info_main.css("display", "block"),
    mask.css("display", "block")
}
  , get_extension = e=>e.split(/[#?]/)[0].split(".").pop().trim().toLocaleLowerCase();
class make_dialogue {
    set_icon(e) {
        e && dia_icon.attr("class", e)
    }
    set_title(e) {
        e && dia_title.text(e)
    }
    set_subtitle(e) {
        dia_subtitle.text(e)
    }
    set_okay_action(action) {
        action && dia_okay.click(()=>{
            eval(action)
        }
        )
    }
    open() {
        dialogue.css("display", "block"),
        mask.css("display", "block")
    }
    close(){
        dialogue.css("display", ""),
        mask.css("display", "");
    }
}
dia_close.click(()=>{
    (new make_dialogue).close();
    if(window.kill_on_close){
        window.close();
        window.kill_on_close = false;
    }
}
),
setTimeout(()=>{
    HOLD_LOAD ? LOAD_DELAY_DONE = !0 : load_hide()
}
, 1e3),
info_open.click(()=>{
    info_build()
}
),
info_close.click(()=>{
    info_main.css("display", ""),
    mask.css("display", "")
}
),
info_refresh.click(()=>{
    info_main.css("display", ""),
    mask.css("display", ""),
    setTimeout(()=>{
        info_build()
    }
    , 100)
}
),
copy.click(()=>{
    prompt("Please copy the link", location.protocol+ "//" + location.host)
}
),
portal_close.click(()=>{
    let e = new make_dialogue;
    e.set_title("Leave portal?"),
    e.set_subtitle("If you exit, remaining file receiving will be canceled and you will leave the portal."),
    e.set_okay_action("(()=>{window.close()})()"),
    e.open()
}
);
const laod_bucket_links = (e,i)=>{
    $("#files_grid").html("");
    if (e && e.length > 0) {
        let t = []
          , a = []
          , s = []
          , l = []
          , n = []
          , o = []
          , _ = 0
          , d = 0
          , c = 0
          , r = 0
          , f = 0
          , p = 0;
        for (x = 0; x < e.length; x++) {
            let i = e[x]
              , m = get_extension(i);
            "png" == m || "jpg" == m || "jpeg" == m || "heic" == m || "raw" == m || "tiff" == m || "webp" == m ? (t[_] = i,
            _++) : "mp3" == m || "wav" == m || "m4a" == m || "ogg" == m || "aac" == m || "alac" == m || "aiff" == m ? (s[c] = i,
            c++) : "mp4" == m || "mkv" == m || "flv" == m || "avi" == m || "webm" == m || "mov" == m ? (a[d] = i,
            d++) : "docx" == m || "doc" == m || "html" == m || "htm" == m || "xml" == m || "svg" == m || "txt" == m || "xls" == m || "xlsx" == m ? (l[r] = i,
            r++) : "apk" == m ? (n[f] = i,
            f++) : (o[p] = i,
            p++)
        }
        make_grid("Images", t),
        make_grid("Videos", a),
        make_grid("Audio", s),
        make_grid("Document", l),
        make_grid("Apps", n),
        make_grid("Files", o),
        start_receive(e, i, 0)
    }
}
  , start_receive = (e,i,t)=>{
    if (!e || !e[t])
        return !1;
    target = e[t],
    window.start_time = (new Date).getTime(),
    load_link(target, i, t, e.length, e);
}
  ,
  setCookie = (cname, cvalue)=>{
    document.cookie = cname + "=" + cvalue + ";path=/";
  }
  ,
  getCookie = (cname)=>{
    let name = cname + "=";
    let decodedCookie = decodeURIComponent(document.cookie);
    let ca = decodedCookie.split(';');
    for(let i = 0; i <ca.length; i++) {
      let c = ca[i];
      while (c.charAt(0) == ' ') {
        c = c.substring(1);
      }
      if (c.indexOf(name) == 0) {
        return c.substring(name.length, c.length);
      }
    }
    return "";
  }
  , c_time = e=>{
    0 == e && (e = 1);
    let i = (e = Math.round(e / 1e3)) + "s";
    return e > 60 && (i = (e = Math.round(e / 60)) + "m"),
    e > 60 && (i = (e = Math.round(e / 60)) + "h"),
    i
}
;
window.c_t = 1;
window.tp = 0;
window.speed_byte_counter = 0;
l_m = 0;
window.last_load = 0;

window.loadedLinks = [];

const load_link = (e,i,t,a,s)=>{
    if(window.loadedLinks.includes(window.hash_set[e])) {
        if(s.length > t + 1){
            load_link(e = s[t + 1], i, t + 1, a, s);
        }
        return;
    };
    let l = new XMLHttpRequest;
    e && e.length > 0 && (l.addEventListener("progress", function(e) {

        window.speed_byte_counter+= (e.loaded-window.last_load);
        window.last_load = e.loaded;
        
        $("#main_d").text("Receiving...(" + window.c_t + "/" + window.c_a + " - "+ f_size(window.TOTAL_BYTES)+")");
        
        diff = (new Date).getTime() - window.start_time;
        ra = Math.round(window.speed_byte_counter/(diff/1000));
    
        ee = ra + 'B/s <i class="fa fa-infinity ii"></i>';
        ii = 'Speed: '+ra+' BPS';

        if(ra > 1024){
            ra = Math.round(ra/1024);
            ee = ra + 'KB/s <i class="fa fa-infinity ii"></i>';
            ii = 'Speed: '+ra+' KBPS';
        }

        if(ra > 1024){
            ra = Math.round(ra/1024);
            ee = ra + 'MB/s <i class="fa fa-infinity ii"></i>';
            ii = 'Speed: '+ra+' MBPS';
        }

        if(ra > 1024){
            ra = Math.round(ra/1024);
            ee = ra + 'GB/s <i class="fa fa-infinity ii"></i>';
            ii = 'Speed: '+ra+' GBPS';
        }

        if(ra > 1024){
            ra = Math.round(ra/1024);
            ee = ra + 'TB/s <i class="fa fa-infinity ii"></i>';
            ii = 'Speed: '+ra+' TBPS';
        }

        MAX_SPEED = ii; 
        $("#speed").html(ee);
    
        let s = e.loaded/e.total,
            a = s*100,
            b = (((s*window.c_t)/window.c_a)*100);
        if(b > window.tp){
        $("#total_progress").css("width", b + "%");
            window.tp = b;
        }

        $("#single_progress").css("width", a + "%")

    }),
    l.addEventListener("readystatechange", function(n) {
        if (2 == l.readyState && 200 == l.status)
            $("#cur_file").text(get_file_name(e)), window.last_load = 0;
        else if (3 == l.readyState) window.c_a = a
        else if (4 == l.readyState) {
            window.loadedLinks.push(window.hash_set[e]);
            window.c_t++;
            if (TOTAL_PACKS = "Total packages: " + s.length,
            DATA_SETS[get_file_name(e)] = l.response,
            window.TOTAL_BYTES+= DATA_SETS[get_file_name(e)].size,
            t + 1 >= a) {
                let e = c_time((new Date).getTime() - window.start_time);
                $("#main_d").text("Done...Total " + (window.c_t-1) + " files received(" + f_size(window.TOTAL_BYTES) + " - " + e + ")"),
                $("#status_icon").html('<i class="fa fa-check-circle ei"></i> Done'),
                init_download()
            } else
                TOTAL_PACKS = "Total packages: " + (t + 1),
                s && s.length > t + 1 && load_link(e = s[t + 1], i, t + 1, a, s)
        }
    }),
    l.addEventListener("onerror", ()=>{
        snack_show("Failed to receive!"),
        TOTAL_PACKS = "Total packages: " + (t + 1),
        s && s.length > t + 1 && load_link(e = s[t + 1], i, t + 1, a, s),
        snack_show("Skipped for error!")
    }
    ));
    if(window.hash_set[e] && window.hash_set[e] != ""){
        l.responseType = "blob";
        l.open("post", "/file/" + i + "/" + window.hash_set[e]);
        l.send();
    }
};

const MAX_FILE_SIZE = 500 * 1024 * 1024;
const ZIP_SIZE_LIMIT = 1000 * 1024 * 1024;
let problematicFiles = [];
let validFiles = [];
let largeFiles = [];
let savedFiles = JSON.parse(localStorage.getItem("savedFiles") || "[]");

const init_download = () => {
    let fileKeys = Object.keys(DATA_SETS);
    
    if (fileKeys.length > 0) {
        snack_show("Saving...", 0);

        validFiles = [];
        largeFiles = [];

        fileKeys.forEach(fileName => {
            
            if (savedFiles.includes(fileName)) {
                return;
            }

            try {
                const fileData = DATA_SETS[fileName];
                const fileSize = fileData.size;

                if (fileSize > MAX_FILE_SIZE) {
                    largeFiles.push(fileName);
                } else {
                    validFiles.push(fileName);
                }
            } catch (error) {
                problematicFiles.push(fileName);
            }
        });

        if (largeFiles.length > 0) {
            largeFiles.forEach(fileName => {
                const fileData = DATA_SETS[fileName];
                saveAs(fileData, fileName);
                savedFiles.push(fileName);
                snack_show(`${fileName} saved separately.`, 0);
            });
        }

        if (validFiles.length > 0) {

            if(validFiles.length === 1){
                const fileData = DATA_SETS[validFiles[0]];
                saveAs(fileData, validFiles[0]);
                savedFiles.push(validFiles[0]);
                snack_show(`Saved ${validFiles[0]}`);
                localStorage.setItem("savedFiles", JSON.stringify(savedFiles));
                return;
            }
            
            let zip = new JSZip();
            let currentZipSize = 0;
            let zipCounter = 1;

            validFiles.forEach(fileName => {
                const fileData = DATA_SETS[fileName];
                currentZipSize += fileData.size;

                if (currentZipSize > ZIP_SIZE_LIMIT) {
                    zip.generateAsync({ type: "blob" }).then(zipBlob => {
                        saveAs(zipBlob, `Received(Sharlet-${zipCounter}).zip`);
                        snack_show(`Saved part ${zipCounter}`);
                        zipCounter++;
                    }).catch(error => {
                        console.error("Error generating ZIP:", error);
                        snack_show("Error saving ZIP. Please check file permissions.");
                    });

                    zip = new JSZip();
                    currentZipSize = fileData.size;
                }

                zip.file(fileName, fileData);
                savedFiles.push(fileName);
            });

            zip.generateAsync({ type: "blob" }).then(zipBlob => {
                saveAs(zipBlob, `Received(Sharlet-${zipCounter}).zip`);
                snack_show(`Saved part ${zipCounter}`);
                localStorage.setItem("savedFiles", JSON.stringify(savedFiles));
            }).catch(error => {
                console.error("Error generating final ZIP:", error);
                snack_show("Error saving final ZIP. Please check file permissions.");
            });
        } else {
            snack_show("No valid files to save.");
            let e1 = new make_dialogue;
            e1.set_title("Save again?"),
            e1.set_subtitle("It seems you have already saved all the files. Do you wish to save again?"),
            e1.set_okay_action("(()=>{localStorage.removeItem('savedFiles');(new make_dialogue).close();savedFiles=[];init_download()})()"),
            e1.open();
        }
    } else {
        snack_show("No files found.");
    }
};