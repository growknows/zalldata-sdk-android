
        (function(para) {
            var p = para.sdk_url, n = para.name, w = window, d = document, s = 'script',x = null,y = null;
            if(typeof(w['zallDataAnalytic201505']) !== 'undefined') {
                return false;
            }
            w['zallDataAnalytic201505'] = n;//'zall'
            w[n] = w[n] || function(a) {return function() {(w[n]._q = w[n]._q || []).push([a, arguments]);}};
            var ifs = ['track','quick','register','registerPage','registerOnce','trackSignup', 'trackAbtest', 'setProfile','setOnceProfile','appendProfile', 'incrementProfile', 'deleteProfile', 'unsetProfile', 'identify','login','logout','trackLink','clearAllRegister','getAppStatus'];
            for (var i = 0; i < ifs.length; i++) {
            w[n][ifs[i]] = w[n].call(null, ifs[i]);
            }
            if (!w[n]._t) {
                x = d.createElement(s), y = d.getElementsByTagName(s)[0];
                x.async = 1;
                x.src = p;
                x.setAttribute('charset','UTF-8');
                w[n].para = para;
                y.parentNode.insertBefore(x, y);
            }
        })({
            sdk_url:'https://869359954.github.io/sadefine/zalldata.full.js',
            name: 'zall',
            is_track_device_id:true,
            source_channel:['bd_vid'],
            source_type:{
                utm:['ls']
            },
            server_url: 'https://sdkdebugtest.datasink.zalldata.cn/sa?project=default&token=cfb8b60e42e0ae9b',
            heatmap:{
                scroll_notice_map:'not_collect',
                element_selector:'not_use_id',
            },
            is_track_single_page:false,
            
            use_app_track:true
            });
            zall.quick('autoTrack');

            
            
