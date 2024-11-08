var qrcode = new QRCode(document.getElementById("qr"), {
    text: "https://example.com",
    width: 500,
    height: 500
});

const fileCount = $('#files-count');
const totalSent = $('#files-count');
const transferSpeed = $('#transfer-speed');
const fileList = document.getElementById('file-list');

const toast = (title = null, body = null, tag = null) => {
    let toastTitle = $('#toast-title');
    let toastBody = $('.toast-body');
    let toastTag = $('#toast-tag');
    if (null == title || null == body) return;
    if (null != tag) {
        toastTag.html(tag);
    }
    toastTitle.html(title);
    toastBody.html(body);
    new bootstrap.Toast(document.getElementById('toast')).show();
};

$(document).ready(() => {
    $('#select-files').click(() => {
        $.get('/select-files', (r) => {
            if (r == '') {
                toast('Unexpected error', 'Failed to trigger file dialogue');
            } else {
                lookForFiles(r);
            }
        }).fail(() => {
            toast('Unexpected error', 'Failed to trigger file dialogue');
        });
    });

    let html_no_files = `
    <div class="alert file-alert" role="alert">
        <i class="fas fa-exclamation-triangle"></i> No files selected
    </div>`;
    fileList.innerHTML = html_no_files;
});

let lookLoop = null;
let newLoad = true;
window.SELECTED_FILES = [];

const lookForFiles = (key) => {
    if (lookLoop !== null) {
        clearTimeout(lookLoop);
        lookLoop = null;
    }
    let url = 'selected-files?key=' + key;
    if(newLoad) url += '&new=true';
    $.get(url, (r) => {
        if (r == 'INVALID_KEY') return;
        else if (r == 'RESET') return;
        else if (r == 'NO_NEW') {
            lookLoop = setTimeout(() => lookForFiles(key), 1000);
            return;
        }
        try {
            let files = JSON.parse(r);
            if (files && files.length > 0) {
                for (let i = 0; i < files.length; i++) {
                    if (!window.SELECTED_FILES.includes(files[i])) {
                        window.SELECTED_FILES.push(files[i]);
                    }
                }
                processFiles();
            }
            if(newLoad) newLoad = false;
        } catch (e) {
            console.error('Error parsing selected files', e);
        }
    }).fail(() => {
        toast('Unexpected error', 'Failed to read selected files');
    });
};

const processFiles = () => {
    console.log(window.SELECTED_FILES);
};
