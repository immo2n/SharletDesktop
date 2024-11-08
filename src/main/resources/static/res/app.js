var qrcode = new QRCode(document.getElementById("qr"), {
    text: "https://example.com",
    width: 500,
    height: 500
});

const fileCount = $('#files-count');
const totalSent = $('#files-count');
const transferSpeed = $('#transfer-speed');

const toast = (title = null, body = null, tag = null)=> {
    let toastTitle = $('#toast-title');
    let toastBody = $('.toast-body');
    let toastTag = $('#toast-tag');
    if(null == title || null == body) return;
    if(null != tag){
        toastTag.html(tag);
    }
    toastTitle.html(title);
    toastBody.html(body);
    new bootstrap.Toast(document.getElementById('toast')).show();
};

$(document).ready(()=> {
    $('#select-files').click(()=> {
        $.get('/select-files', (r)=> {
            if(r != 'TRIGGERED_FILE_DIALOGUE'){
                toast('Unexpected error', 'Failed to trigger file dialogue');
            }
        }).fail(()=> {
            toast('Unexpected error', 'Failed to trigger file dialogue');
        });
    });
});