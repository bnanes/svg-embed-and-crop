function addListener(element, type, callback) {
 if (element.addEventListener) element.addEventListener(type, callback);
 else if (element.attachEvent) element.attachEvent('on' + type, callback);
}
var spz = document.getElementById('svg-embed-crop-zip');
addListener(spz, 'click', function() {
  ga('send', 'event', 'download', 'svg-embed-crop-zip');
});
var gh = document.getElementById('git');
addListener(gh, 'click', function() {
  ga('send', 'event', 'follow', 'github');
});
