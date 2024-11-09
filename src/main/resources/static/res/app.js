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


let html_no_files = `
<div class="alert file-alert" role="alert">
    <i class="fas fa-exclamation-triangle"></i> No files selected
</div>`;

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

    fileList.innerHTML = html_no_files;
});

let lookLoop = null;
let newLoad = true;
let listEmpty = true;

const lookForFiles = (key) => {
    if (lookLoop !== null) {
        clearTimeout(lookLoop);
        lookLoop = null;
    }
    let url = 'selected-files?key=' + key;
    if (newLoad) url += '&new=true';
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
                processFiles(files);
            }
            if (newLoad) newLoad = false;
        } catch (e) {
            console.error('Error parsing selected files', e);
        }
    }).fail(() => {
        toast('Unexpected error', 'Failed to read selected files');
    });
};


let filesArray = [];
let displayLimit = 10;
let currentIndex = 0;

const calculateDisplayLimit = () => {
    const listHeight = fileList.clientHeight;
    displayLimit = Math.floor(listHeight / 50) + 5;
    displayLimit*= 2;
};

calculateDisplayLimit();

window.addEventListener('resize', () => {
    calculateDisplayLimit();
});

const loadMoreFiles = (scrollingDown = true) => {
    const endIndex = Math.min(currentIndex + displayLimit, filesArray.length);
    if (scrollingDown) {
        for (let i = currentIndex; i < endIndex; i++) {
            if (!document.getElementById('f_' + i)) {
                const file = filesArray[i];
                let fileItem = document.createElement('div');
                fileItem.classList.add('file-item');
                fileItem.setAttribute('id', 'f_' + i);
                fileItem.innerHTML = `
                    <div>
                        <span>${file.name}</span>
                    </div>
                    <div>
                        <button class="remove-file" data-index="${i}">
                            <i class="fa fa-times"></i>
                        </button>
                    </div>
                `;
                fileList.appendChild(fileItem);
            }
        }

        while (fileList.children.length > displayLimit) {
            fileList.removeChild(fileList.firstChild);
        }

        currentIndex = endIndex;
    } else {
        
        const startIndex = Math.max(currentIndex - displayLimit, 0);

        for (let i = currentIndex - 1; i >= startIndex; i--) {
            if (!document.getElementById('f_' + i)) {
                const file = filesArray[i];
                let fileItem = document.createElement('div');
                fileItem.classList.add('file-item');
                fileItem.setAttribute('id', 'f_' + i);
                fileItem.innerHTML = `
                    <div>
                        <span>${file.name}</span>
                    </div>
                    <div>
                        <button class="remove-file" data-index="${i}">
                            <i class="fa fa-times"></i>
                        </button>
                    </div>
                `;
                fileList.prepend(fileItem);
            }
        }        

        while (fileList.children.length > displayLimit) {
            fileList.removeChild(fileList.lastChild);
        }

        let list = $('#file-list .file-item');
        let first = list[0];

        if($(first).attr('id') !== 'f_0') {
            fileList.scrollTop = 150;
        }

        currentIndex = startIndex;

    }

    // Attach remove button event listeners
    $('.remove-file').off('click').on('click', (event) => {
        const fileIndex = $(event.target).data('index');
        removeFile(fileIndex);
    });
};

const removeFile = (index) => {
    filesArray.splice(index, 1);
    resetFileList();
};

const resetFileList = () => {
    currentIndex = 0;
    fileList.innerHTML = '';
    loadMoreFiles();
};

fileList.addEventListener('scroll', () => {
    const scrollTop = fileList.scrollTop;
    const clientHeight = fileList.clientHeight;
    const scrollHeight = fileList.scrollHeight;

    if (scrollTop + clientHeight >= scrollHeight * 0.8) {
        loadMoreFiles(true);
    } 
    else if (scrollTop <= scrollHeight * 0.2) {
        loadMoreFiles(false);
    }
});


fileList.innerHTML = html_no_files;

const processFiles = (files) => {
    if (filesArray.length === 0) {
        fileList.innerHTML = '';
        listEmpty = false;
    }
    
    filesArray.push(...files);
    
    if (currentIndex === 0) {
        loadMoreFiles();
    }
};
