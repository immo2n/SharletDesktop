snack = $("#snackbar");
const c = $(".connecting")
  , svg = $("#load_svg")
  , done = $("#con_done")
  , open_rec = e=>{
    document.cookie = "received=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
    svg.remove(),
    done.css("display", "flex"),
    $("#auth, #mask").remove();
    window.open("/x-" + e + "/receive/"),
    c.html('<a href="/x-' + e + '/receive/" target="_blank">Click to open receiver</a>')
}
  , rest_p = ()=>{
    svg.remove(),
    $("#con_done i").attr("class", "fa fa-exclamation"),
    c.text("Portal is restricted!"),
    done.css("display", "flex")
}
;
$("#auth_pin").on("keypress", e=>{
    "Enter" === e.key && (e.preventDefault(),
    $("#auth_go").click())
}
);
const snack_show = e=>{
    window.st && clearTimeout(window.st),
    snack.text(e),
    snack.addClass("show"),
    window.st = setTimeout(()=>{
        snack.removeClass("show"),
        window.st = !1
    }
    , 2900)
}
;
let attempts = 5;
const auth_err = ()=>{
    $("#auth_pin").val(""),
    $("#auth_go").html("Open portal"),
    snack_show("Try again!"),
    attempts--
}
;
setTimeout(()=>{
    $("#mask, #auth").css("display", "block"),
    $("#auth_pin").focus()
}
, 200),
$("#auth_go").click(()=>{
    let e = $("#auth_pin").val()
      , t = $("#auth_go");
    if (attempts <= 0) {
        $("#auth").remove(),
        $("#mask").remove(),
        rest_p();
        return
    }
    if (!e || "" == e) {
        snack_show("Enter correct pin please!");
        return
    }
    t.html('<i class="fa fa-circle-notch fa-spin"></i>'),
    $.post("/auth", {p: e}).done(t=>{
        (t=="PASS")?open_rec(e):auth_err();
    }
    ).fail(()=>{
        auth_err()
    }
    )
}
);
