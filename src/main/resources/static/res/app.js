const fileCount = $('#files-count');
const totalSent = $('#files-count');
const transferSpeed = $('#transfer-speed');
const fileList = document.getElementById('file-list');
const appStatus = $('#status');

const html_markup_status_ready = '<i class="fa-solid fa-check-circle"></i> Ready to send';
appStatus.html(html_markup_status_ready);

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

let html_no_files_found = `
<div class="alert file-alert" role="alert">
    <i class="fas fa-search"></i> No files found!
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

    $('#clear-all').click(() => {
        Swal.fire({
            title: "Are you sure?",
            text: "All the selected files will be cleared, but they are not deleted from your device and you can select them again.",
            icon: "warning",
            showCancelButton: true,
            confirmButtonColor: "#3085d6",
            cancelButtonColor: "#d33",
            confirmButtonText: "Clear all"
        }).then((result) => {
            if (result.isConfirmed) {
                $.get('/clear-all').done((d)=> {
                    if(d === 'OK') {
                        filesArray = [];
                        resetFileList();
                        Swal.fire({
                            title: "Cleared!",
                            text: "Your file has been cleared. You can select new files now.",
                            icon: "success"
                        });
                    }
                    else {
                        toast('Unexpected error', 'Failed to clear selected files');
                    }
                }).fail(() => {
                    toast('Unexpected error', 'Failed to clear selected files');
                });
            }
        });
    });

    $('#search').on('input', () => {
        fileList.innerHTML = '';
        const query = $('#search').val().toLowerCase();
        if (query.length === 0){
            fileList.innerHTML = '';
            resetFileList(true);
            return;
        }
        else filesArray.innerHTML = '';
        const filteredFiles = filesArray.filter((file) => file.name.toLowerCase().includes(query));
        if(filteredFiles.length === 0) {
            fileList.innerHTML = html_no_files_found;
            return;
        }
        for(let i = 0; i < filteredFiles.length; i++) {
            const file = filteredFiles[i];
            const fileItem = document.createElement('div');
            fileItem.classList.add('file-item');
            makeFileItem(file, fileItem);
            fileList.appendChild(fileItem);
        }
    });

    $('#clear-search').click(() => {
        $('#search').val('');
        fileList.innerHTML = '';
        resetFileList(true);
    }
);
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

fileList.innerHTML = html_no_files;

const calculateDisplayLimit = () => {
    const listHeight = fileList.clientHeight;
    displayLimit = Math.floor(listHeight / 50) + 5;
};
calculateDisplayLimit();

window.addEventListener('resize', calculateDisplayLimit);

const loadFilesChunk = () => {
    fileCount.text(filesArray.length);
    const endIndex = Math.min(currentIndex + displayLimit, filesArray.length);

    for (let i = endIndex - 1; i >= currentIndex; i--) {
        const file = filesArray[i];
        const fileItem = document.createElement('div');
        fileItem.classList.add('file-item');
        makeFileItem(file, fileItem);
        fileList.appendChild(fileItem);
    }
    currentIndex = endIndex;

    if (filesArray.length === 0) {
        fileList.innerHTML = html_no_files;
    }
};


fileList.addEventListener('scroll', () => {
    const scrollTop = fileList.scrollTop;
    const clientHeight = fileList.clientHeight;
    const scrollHeight = fileList.scrollHeight;

    if (scrollTop + clientHeight >= scrollHeight * 0.9 && currentIndex < filesArray.length) {
        loadFilesChunk();
    }
});

const formatSize = (bytes)=> {
     bytes = Number(bytes);
     if (isNaN(bytes)) return "0 B";
     const units = ['B', 'KB', 'MB', 'GB', 'TB', 'PB'];
     let unitIndex = 0;
     while (bytes >= 1024 && unitIndex < units.length - 1) {
         bytes /= 1024;
         unitIndex++;
     }
     return `${bytes.toFixed(2)} ${units[unitIndex]}`;
};

const makeFileItem = (file, item) => {

    const iconMap = {
        'pdf': 'fa-file-pdf',
        'doc': 'fa-file-word', 'docx': 'fa-file-word',
        'xls': 'fa-file-excel', 'xlsx': 'fa-file-excel',
        'ppt': 'fa-file-powerpoint', 'pptx': 'fa-file-powerpoint',
        'jpg': 'fa-file-image', 'jpeg': 'fa-file-image', 'png': 'fa-file-image',
        'gif': 'fa-file-image', 'bmp': 'fa-file-image', 'svg': 'fa-file-image', 'webp': 'fa-file-image',
        'mp3': 'fa-file-audio', 'wav': 'fa-file-audio', 'aac': 'fa-file-audio', 'flac': 'fa-file-audio',
        'mp4': 'fa-file-video', 'avi': 'fa-file-video', 'mov': 'fa-file-video', 'mkv': 'fa-file-video', 'webm': 'fa-file-video',
        'zip': 'fa-file-archive', 'rar': 'fa-file-archive', '7z': 'fa-file-archive', 'tar': 'fa-file-archive', 'gz': 'fa-file-archive',
        'txt': 'fa-file-text', 'md': 'fa-file-text', 'log': 'fa-file-text', 'csv': 'fa-file-csv',
        'html': 'fa-file-code', 'htm': 'fa-file-code', 'xml': 'fa-file-code',
        'js': 'fa-file-code', 'json': 'fa-file-code', 'ts': 'fa-file-code',
        'css': 'fa-file', 'scss': 'fa-file', 'less': 'fa-file',
        'py': 'fa-file-code', 'java': 'fa-file-code', 'c': 'fa-file-code',
        'cpp': 'fa-file-code', 'rb': 'fa-file-code', 'php': 'fa-file-code',
        'exe': 'fa-file', 'bin': 'fa-file', 'sh': 'fa-file', 'bat': 'fa-file',
        'default': 'fa-question-circle'
    };

    const iconClass = iconMap[file.name.split('.').pop().toLowerCase()] || iconMap['default'];
    item.innerHTML = `
        <div class="file-info">
            <div class="icon"><i class="fa ${iconClass}"></i></div>
            <div class="name">
                <div class="full-name" onclick="openFile('${file.hash}')" title="Preview file">${file.name}</div>
                <div class="size">${formatSize(file.size)}</div>
            </div>
        </div>
        <div>
            <button class="remove-file" title="Remove file">
                <i class="fa fa-times"></i>
            </button>
        </div>
    `;
    item.querySelector('.remove-file').addEventListener('click', () => removeFile(file.hash, item));
};

const openFile = (hash) => {
    $.get('/preview', {hash: hash}).done((d) => {
        if(d !== 'OK'){
        if(d === 'PREVIEW_NOT_SUPPORTED'){
            toast('Preview not supported', 'This file type is not supported for preview');
        }
        else if(d === 'FILE_NOT_FOUND'){
            toast('File not found', 'The file you are trying to open is not found');
        }
        else toast('Unexpected error', 'Failed to open file');
    }
    }).fail(() => {
        toast('Unexpected error', 'Failed to open file');
    });
};

const processFiles = (files) => {
    if (filesArray.length === 0) fileList.innerHTML = '';
    filesArray.push(...files);
    loadFilesChunk();
};

const removeFile = (hash, item) => {
    let file = filesArray.find(f => f.hash === hash);
    if (file) {
        let index = filesArray.indexOf(file);
        $.get('/clear', {hash: file.hash}).done((d) => {
            if (d === 'OK') {
                filesArray.splice(index, 1);
                $(item).remove();
                fileCount.text(filesArray.length);
                if(filesArray.length === 0) resetFileList();
            } else {
                toast('Unexpected error', 'Failed to remove file');
            }
        }).fail(() => {
            toast('Unexpected error', 'Failed to remove file');
        });
    } else $(item).remove();
};


const resetFileList = (searchMdde = false) => {
    currentIndex = 0;
    if(!searchMdde) fileList.innerHTML = html_no_files;
    loadFilesChunk();
};

loadFilesChunk();

$(document).ready(()=> {
    $.get('/pin').done((d) => {
        $("#mainLink").html('<i class="fas fa-link"></i> http://192.168.0.198:5566<br><i class="fas fa-key"></i> '+d);
        new QRCode(document.getElementById("qr"), {
            text: "http://192.168.0.198:5566/x-" + d + "/receive/",
            width: 500,
            height: 500
        });
    });
});