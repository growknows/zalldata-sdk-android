(function (para) {
    var p = para.sdk_url,
        n = para.name,
        w = window,
        d = document,
        s = 'script',
        x = null,
        y = null;
    if (typeof (w['zallDataAnalytic201505']) !== 'undefined') {
        return false;
    }
    w['zallDataAnalytic201505'] = n; //'zall'
    w[n] = w[n] || function (a) {
        return function () {
            (w[n]._q = w[n]._q || []).push([a, arguments]);
        }
    };
    var ifs = ['track', 'quick', 'register', 'registerPage', 'registerOnce', 'trackSignup', 'trackAbtest', 'setProfile', 'setOnceProfile', 'appendProfile', 'incrementProfile', 'deleteProfile', 'unsetProfile', 'identify', 'login', 'logout', 'trackLink', 'clearAllRegister', 'getAppStatus'];
    for (var i = 0; i < ifs.length; i++) {
        w[n][ifs[i]] = w[n].call(null, ifs[i]);
    }
    // var obtn = document.getElementById('test');
    // console.log(obtn);
    // obtn.onclick = function(){
    //     x = d.createElement(s), y = d.getElementsByTagName(s)[0];
    //     x.async = 1;
    //     x.src = p;
    //     x.setAttribute('charset','UTF-8');
    //     w[n].para = para;
    //     y.parentNode.insertBefore(x, y);
    // };
    if (!w[n]._t) {
        x = d.createElement(s), y = d.getElementsByTagName(s)[0];
        x.async = 1;
        x.src = p;
        x.setAttribute('charset', 'UTF-8');
        w[n].para = para;
        y.parentNode.insertBefore(x, y);
    }
})({
    sdk_url:'./zalldata.full.js',
    // sdk_url: 'https://869359954.github.io/sadefine/zalldata.full.js',
    // sdk_url:'https://cdn.jsdelivr.net/npm/za-sdk-javascript@1.14.20/zalldata.min.js',
    // heatmap_url: 'https://cdn.jsdelivr.net/npm/za-sdk-javascript@1.14.21/heatmap.min.js',
    // sdk_url: './sdk-919/zalldata.full.js',
    // heatmap_url: './zall/heatmap.js',
    name: 'zall',
    is_track_device_id: true,
    source_channel: ['bd_vid'],
    source_type: {
        search: ['.baidu.com', '.google.', 'ecosia.org'],
        social: ['.kaixin001.com']
    },
    // server_url:'',
    server_url: 'https://sdkdebugtest.datasink.zalldata.cn/sa?project=default&token=cfb8b60e42e0ae9b',
    // server_url: ['https://test-syg.datasink.zalldata.cn/sa?project=liangshuang&token=27f1e21b78daf376','https://test-syg.datasink.zalldata.cn/sa?project=lixiang&token=27f1e21b78daf376'],
    debug_mode: false,
    debug_mode_upload: false,
    // send_type:'ajax',
    // use_app_track: true,
    use_app_track_config:{
        use_app_track:true,
        white_list:['https://sdkdebugtest.datasink.zalldata.cn/sa?project=default&token=cfb8b60e42e0ae9b',
                    'http://test-syg.datasink.zalldata.cn/sa?project=liangshuang&token=27f1e21b78daf376',
                    'https://newsdktest.datasink.zalldata.cn/sa?project=zhangwei&token=5a394d2405c147ca']
    },
    use_client_time: true,
    cross_subdomain: false,
    // batch_send:{
    //     datasend_timeout: 6000,  //一次请求超过多少秒的话自动取消，防止请求无响应。
    //     send_interval: 12000,    //间隔几秒发一次数据。
    //     one_send_max_length: 6  //一次请求最大发送几条数据，防止数据太大
    // },
    is_track_single_page: false,
    // heatmap:{},
    heatmap: {
        // clickmap:'not_collect',
        // scroll_notice_map:'not_collect',
        loadTimeout: 3000,
        // collect_url: function(){
        //     //如果只采集首页
        //     if(location.href === 'http://www.ls.com:8080/index.html'){
        //         return false;
        //     }else{
        //         return true;
        //     }
        // },
        collect_element: function (element_target) {
            // 如果这个元素有属性zall-disable=true时候，不采集
            if (element_target.getAttribute('zall-disable') === 'true') {
                return false;
            } else {
                return true;
            }
        },
        custom_property: function (element_target) {
            return {
                timepppp: new Date()
            }
        },
        collect_input: function (element_target) {
            //例如如果元素的 id 是a，就采集这个元素里的内容
            // if(element_target.id === 'loginid'){
            //     return true;
            // }
            return true;
        },
        // element_selector:'not_use_id',
        renderRefreshTime: 1000,
        scroll_delay_time: 4000,
        useCapture: false
    },
    // preset_properties:{
    //     title:false
    // }
});
// zall.registerPage({
//      $title : '123tt'
// });

// zall.quick('autoTrackWithoutProfile');
zall.quick('autoTrack', {
    $title: 'mytest'
});
zall.track('sdk_1152', {});
// zall.quick('isReady',function(){
//     var pro = zall.getPresetProperties();
//     console.log(pro);
// })
// zall.setProfile()
// window.ZallData_APP_JS_Bridge = {
//     zalldata_define_mode : function(data){
//     //    var data = JSON.parse(data);
//         console.log(data);

//     }
// };

// var webkit = {
//     messageHandlers :{
//         zalldataNativeTracker : {
//             postMessage : function(data){
//                 console.log('ios 成功接收数据');
//                 console.log(data);
//             }
//         }
//     }
// };
// var ZallData_iOS_JS_Bridge = {
//     zalldata_app_server_url: 'http://test-syg.datasink.zalldata.cn/sa?project=liangshuang&token=27f1e21b78daf376',
// };

// var ZallData_APP_JS_Bridge = {
//     // zalldata_track : function(data){
//     //     console.log('android接收数据',data);
//     // },
//     // zalldata_get_server_url:function(){
//     //     return 'https://test-syg.datasink.zalldata.cn/sa?project=liangshuang&token=27f1e21b78daf376';
//     // }
//     // zalldata_verify:function(data){
//     //     console.log('android verify 接收数据',data);
//     //     return true;
//     // }
// };
