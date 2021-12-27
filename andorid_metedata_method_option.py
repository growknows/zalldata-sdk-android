import os
import sys
import re
import random
import argparse
import subprocess
import shutil
import gzip


# 遍历文件夹
def dir_option(path):
    file_lists = list()
    if os.path.isdir(path):
        sub_dirs = os.listdir(path)
        for dir in sub_dirs:
            dir_path = os.path.join(path, dir)
            if os.path.isfile(dir_path):
                base_name, file_type = os.path.splitext(dir_path)
                if file_type in file_types():
                    file_lists.append(dir_path)
            elif os.path.isdir(dir_path):
                if dir not in filter_file_types():
                    # 这里修改文件夹名称
                    new_path = dir_path
                    dict = dir_filter_types()
                    dir_suffix = os.path.splitext(dir)[1]
                    if dir in dict.keys():
                        new_path = os.path.join(path, dict[dir])
                        os.rename(dir_path, new_path)
                    elif dir_suffix in file_dir_types():
                        new_name = dict.get(os.path.splitext(dir)[0])
                        if new_name:
                            new_name += dir_suffix
                            new_path = os.path.join(path, new_name)
                            os.rename(dir_path, new_path)
                    file_lists += dir_option(new_path)
    elif os.path.isfile(path):
        file_lists.append(path)
    return file_lists


# 操作文件
def file_option(file_paths, plugin=False):
    dict_class = dict()
    # p_file_paths = tqdm(file_paths)
    for file_path in file_paths:
        base_name, file_type = os.path.splitext(file_path)
        if file_type in file_types() and file_type not in file_ban_file():
            file_name = os.path.basename(base_name).strip()
            if dict_class.get(file_name) is None:
                dict_class[file_name] = random_data()
            # 修改文本内容
            file_name_modify_note(file_path, dict_class, plugin, file_type)
        # 修改文件名
        file_name_chaing(file_path, plugin)
        # p_file_paths.update(1)


# 全部修改功能
# 修改注释相关
# Created by * on -> Created by guo on
# 2021/11/11. -> 随机
# 神策 -> 卓尔
def file_name_modify_note(file_path, dict, plugin, file_type):
    read_all = None
    if os.path.isfile(file_path):
        with open(file_path, encoding='UTF-8') as f:
            read_all = f.read()
            f.close()
        new_read_all = read_all
        new_read_all = file_name_modify_founder(new_read_all)
        if file_type == ".gradle":
            new_read_all = file_content_modify_method(
                new_read_all, replace_gradle_keyword())
            new_read_all = file_name_gradle_ching(new_read_all, file_path)
            if plugin:
                new_read_all = file_content_modify_method(
                    new_read_all, replace_plugin2_keyword())

        elif last_package is False:

            keys = replace_plugin2_keyword() if plugin else replace_keyword()
            new_read_all = file_content_modify_method(new_read_all, keys)
            new_read_all = file_compatible_modify_class_method(new_read_all)
        if read_all != new_read_all:
            with open(file_path, 'w', encoding='UTF-8') as f:
                f.writelines(new_read_all)
                f.close()
    else:
        print("❌❌❌❌❌ -> : "+file_path)


# 修改创建人
def file_name_modify_founder(content):
    str_match = re.search(r"Created by (.*) on", content.strip())
    if str_match and str_match.group():
        author = str_match.groups()[0]
        if author != "guo":
            old_str = str_match.group()
            new_str = old_str.replace(author, "guo")
            content = content.replace(old_str, new_str, 1)

    str_match = re.search(r"Copyright (.*) Sensors Data Inc.", content.strip())
    if str_match and str_match.group():
        old_str = str_match.group()
        new_str = old_str.replace("Sensors Data", "Zall Data")
        content = content.replace(old_str, new_str, 1)

    return content


# 修改时间
def file_name_modify_time(file_path, content, dict):
    str_match = re.search(r"(\d){4}\/(\d){1,2}\/(\d){1,2}\.", content.strip())
    if str_match is not None and str_match.group() is not None:
        old_str = str_match.group().strip()
        class_name = os.path.basename(os.path.splitext(file_path)[0])
        new_str = dict[class_name].strip()+"."
        content = content.replace(old_str, new_str, 1)
    return content


# 兼容sa H5操作
def file_compatible_modify_class_method(content):
    dict = replace_h5_keyword()
    for key, value in dict.items():
        newValue = value if args.h5 else key
        content = content.replace(key, newValue)
        content = content.replace(value, newValue)

    return content


# 修改文件内容
def file_content_modify_method(content, dicts):
    for key, value in dicts.items():
        content = content.replace(key, value)
    return content


# 文件名称修改
def file_name_chaing(file_path, plugin):
    if last_package:
        return
    current_dir = os.path.dirname(file_path)
    old_file_name = os.path.basename(file_path)
    new_file_name = old_file_name

    dict = replace_plugin2_keyword() if plugin else replace_keyword()
    for key, value in dict.items():
        replace_file_name = new_file_name.replace(key, value)
        if replace_file_name != old_file_name:
            new_file_name = replace_file_name

    if new_file_name:
        new_file_path = os.path.join(current_dir, new_file_name)
        os.rename(file_path, new_file_path)


# 修改gradle
def file_name_gradle_ching(content, file_path):
    file_name = os.path.basename(file_path)
    path_name = os.path.basename(os.path.dirname(file_path))
    if 'demo' in path_name and file_name == "build.gradle":
        repositories = """
repositories {  
    flatDir {  
        dirs 'libs'  
    }  
}  
        """
        content = content.replace(repositories, '')
        if args.packag and last_package:
            content = content + repositories

    return content


#  随机日期
def random_data():
    year = random.randint(2020, 2021)
    moon = random.randint(1, 12)
    max_day = 28
    if moon in [1, 3, 5, 7, 8, 10, 12]:
        max_day = 31
    elif moon in [4, 6, 9, 11]:
        max_day = 30
    day = random.randint(1, max_day)
    if year == 2021 or moon == 12:
        moon = random.randint(1, 11)
    return str(str(year)+'/'+str(moon)+'/'+str(day))


# 关键字 越多性能越差...
def replace_keyword():
    return {
        "SensorsAnalyticsSDK-Info": args.project_name+"-Info",
        "SensorsAnalyticsExtension-Info": args.project_name+"Extension-Info",
        "SensorsAnalyticsTests": args.project_name+"Tests",
        "SensorsAnalyticsExtension": args.project_name+"Extension",
        "SensorsAnalyticsSDK": args.project_name,
        "SensorsAnalytics": "ZallAnalytics",
        "Sensors Analytics": "Zall Analytics",
        "sensors_analytics_": "zall_analytics_",
        "sensorsdata.analytics": "zalldata.analytics",
        "sensorsdata_": "zalldata_",
        "sensorsdata": "zalldata",
        "SENSORSDATA": "ZALLDATA",
        "SensorsData": "ZallData",
        "SENSORS": "ZALL",
        "Sensors": "Zall",
        "sensors": "zall",
        # 小写sa
        "saweb": "zaweb",
        "sajs": "zajs",
        "(sa)": "(za)",
        "\"sa\"": "\"za\"",
        "\'sa\'": "\'za\'",
        "sa.": "za.",
        "sa-": "za-",
        "\/sa": "\/za-",
        "sa_": "za_",
        # class
        "kSA": "kZA",
        "SAA": "ZAA",
        "SABase": "ZABase",
        "SABridge": "ZABridge",
        "SABOOL": "ZABOOL",
        "SACo": "ZACo",
        "SACa": "ZACa",
        "SACh": "ZACh",
        "SACAID": "ZACAID",
        "SACu": "ZACu",
        "SACell": "ZACell",
        "SACryptopp": "ZACryptopp",
        "SAClass": "ZAClass",
        "SACert": "ZACert",
        "SAClick": "ZAClick",
        "SACG": "ZACG",
        "SAD": "ZAD",
        "SADa": "ZADa",
        "SAEvent": "ZAEvent",
        "SAECC": "ZAECC",
        "SAEE": "ZAEE",
        "SAEncryptP": "ZAEncryptP",
        "SAEncryptM": "ZAEncryptM",
        "SAEncryptE": "ZAEncryptE",
        "SAEncryptT": "ZAEncryptT",
        "SAEncryptV": "ZAEncryptV",
        "SAEncryptR": "ZAEncryptR",
        "SAEncryptS": "ZAEncryptS",
        "SAElement": "ZAElement",
        "SAEnum": "ZAEnum",
        "SAException": "ZAException",
        "SAF": "ZAF",
        "SAGesture": "ZAGesture",
        "SAGzip": "ZAGzip",
        'SAGeneral': "ZAGeneral",
        "SAH": "ZAH",
        "SAI": "ZAI",
        "SAJ": "ZAJ",
        "SAKey": "ZAKey",
        "SALogger": "ZALogger",
        "SAL": "ZAL",
        "SAM": "ZAM",
        "SANet": "ZANet",
        "SAO": "ZAO",
        "SAPr": "ZAPr",
        "SAPa": "ZAPa",
        "SAPu": "ZAPu",
        "SAPost": "ZAPost",
        "SARSAEncryptor": "ZARSAEncryptor",
        "SARe": "ZARe",
        "SARSA": "ZARSA",
        "SARCT": "ZARCT",
        "SARN": "ZARN",
        # "SAS": "ZAS",  # 注意iOS->SASecretKey
        "SASecretKey": "ZASecretKey",
        "RZASecretKey:": "RSASecretKey:",
        "SASu": "ZASu",
        "SASw": "ZASw",
        "SASc": "ZASc",
        "SASS": "ZASS",
        "SASo": "ZASo",
        "SASi": "ZASi",
        "SASec": "ZASec",
        "SASDK": "ZASDK",
        "SASegment": "ZASegment",
        "SAServer": "ZAServer",
        "SASequence": "ZASequence",
        "SAT": "ZAT",
        "SAUI": "ZAUI",
        "SAUR": "ZAUR",
        "SAUN": "ZAUN",
        "SAUd": "ZAUd",
        "SAUtilsTest": "ZAUtilsTest",
        "SAV": "ZAV",
        "SAW": "ZAW",
        "\"SA ": "\"ZA ",
        " SA ": " ZA ",
        "SA_": "ZA_",
        "SA.": "ZA.",
        "+SA": "+ZA",
        "SA%@": "ZA%@",
        "Sa_": "Za_",
        # 企业名称
        "神策": "卓尔",

        # iOS 异常处理
        ".za_": ".sa_",
        "ZA_SIGINFO": "SA_SIGINFO",
    }


def replace_gradle_keyword():
    gradle_key = {
        "https://maven.google.com": "https://maven.aliyun.com/repository/google",
        "file:/Users/wangzhuozhou/Documents/repo": f"../{option_folder()[0]}/repo",
        "https://dl.bintray.com/zouyuhan/maven": f"../{option_folder()[0]}/repo",
        "com.sensorsdata.analytics.android:android-gradle-plugin2": "com.zalldata.analytics.android:android-gradle-plugin",
        "sensors_analytics_": "zall_analytics_",
        "com.sensorsdata.analytics.android": "com.zalldata.analytics.android",
        "SensorsAnalyticsSDK": args.project_name,
        "Sensors Analytics.": "Zall Analytics.",
        "sensorsdata": "zalldata",
        "plugin:3.4.0": "plugin:3.4.4",
        "SENSORSDATA": "ZALLDATA",
        "sensorsAnalytics": "zallAnalytics",
    }
    package_dict = {
        "implementation project(':SensorsAnalyticsSDK')": f"compile(name:\"{args.project_name}\",ext:\"aar\")",
        f"implementation project(':{args.project_name}')": f"compile(name:\"{args.project_name}\",ext:\"aar\")",
        "https://dl.bintray.com/zouyuhan/maven": f"./plugin_sdk",
        f"../{option_folder()[0]}/repo": f"./plugin_sdk"
    }
    if args.packag and last_package:
        gradle_key.update(package_dict)
    else:
        gradle_key.update({k: v for v, k in package_dict.items()})
    return gradle_key


def replace_plugin2_keyword():
    return {
        "https://oss.sonatype.org/service/local/staging/deploy/maven2/": "../repo",
        "sensorsAnalytics": "zallAnalytics",
        "SensorsAnalytics": "ZallAnalytics",
        "sensorsdata": "zalldata",
        "sensorsData": "zallData",
        "SensorsData": "ZallData",
        "sensors": "zall",
        "Sensors": "Zall",
        "SENSORS": "ZALL",
        "android-gradle-plugin2": "android-gradle-plugin",
        "sa-sdk-android-plugin2": "za-sdk-android-plugin",
        "SALog": "ZALog",
        "gradle-5.1.1-bin.zip": "gradle-6.5-bin.zip",
        "ASM_VERSION": "ZSM_VERSION",
        "SALog": "ZALog",
        'sALog': "zALog"
    }


# 文件类型s
def file_types():
    return [
        '.gradle',
        '.groovy',
        '.java',
        '.properties',
        '.xml',
        '.kt',
        '.md',
        '.js',
        '.html',
        '.pro'
    ]


# 禁止修改内容的类型
def file_ban_types():
    return [
        '.json',
    ]


# 禁止修改某个指定文件内容
def file_ban_file():
    return[
        'sa_mcc_mnc_mini',
        os.path.splitext(os.path.basename(__file__))[0]
    ]


# 带后缀的文件夹
def file_dir_types():
    return [

    ]


# 过滤文件文件夹类型
def filter_file_types():
    return [
        ".git",
        ".idea",
        "build",
        ".gitignore",
    ]


# 修改文件夹名称
def dir_filter_types():
    return {
        "SensorsAnalyticsSDK": args.project_name,
        "SensorsAnalyticsTests": args.project_name+"Tests",
        "SensorsAnalyticsExtension": args.project_name+"Extension",
        "SensorsData": "ZallData",
        "SensorsDataExtention": "ZallDataExtention",
        "SensorsDataSwift": "ZallDataSwift",
        "SALogger": "ZALogger",
        "sensorsdata": "zalldata",
        "sa-sdk-android": "za-sdk-android",

    }


# 兼容神策h5关键字
def replace_h5_keyword():
    return {
        "zalldata_app_js_bridge_call_js": "sensorsdata_app_js_bridge_call_js",
        "zallanalytics://getAppInfo": "sensorsanalytics://getAppInfo",
        "zallanalytics://trackEvent": "sensorsanalytics://trackEvent",
        "ZallData_APP_JS_Bridge": "SensorsData_APP_JS_Bridge",
        "zalldata_call_app": "sensorsdata_call_app",
        "ZallData_iOS_JS_Bridge": "SensorsData_iOS_JS_Bridge",
        "zalldata_app_server_url": "sensorsdata_app_server_url",
        "ZallData_App_Visual_Bridge": "SensorsData_App_Visual_Bridge",
        "zalldata_visualized_mode": "sensorsdata_visualized_mode",
        "ZallData_APP_New_H5_Bridge": "SensorsData_APP_New_H5_Bridge",
        "zalldata_get_app_visual_config": "sensorsdata_get_app_visual_config",
        "zalldata_app_call_js": "sensorsdata_app_call_js",
        "zalldata-check-jssdk": "sensorsdata-check-jssdk",
        "zallDataAnalytic201505": "sensorsDataAnalytic201505",
        "static.zalldata.cn": "0.0.0.0:3000",
        "za-sdk-ios": "sa-sdk-ios",
    }


# 配置打包
def auto_packag():
    current_path = os.path.dirname(args.path)
    plugin_path = os.path.join(current_path, option_folder()[0])
    sdk_project_path = args.path
    # plugin 需要提前编译.若是sdk需要编译则plugin则会强制编译.而不需依赖args.plugin值
    packag = 1 if args.packag == 1 else args.plugin
    cmds = ["gradle uploadarchives", "gradle assembleRelease"]

    result = False
    if packag:
        result = shell_script_execute(plugin_path, cmds[0])
    if args.packag and result:
        result = shell_script_execute(sdk_project_path, cmds[1])
        global last_package
        last_package = True

    if args.packag and result and last_package:
        paths = dir_option(sdk_project_path)
        file_option(paths, 0)
        os.chdir(current_path)
        packag_path = os.path.join(sdk_project_path, "plugin_sdk")
        if os.path.isdir(packag_path):
            shutil.rmtree(packag_path)
        os.mkdir(packag_path)

        plugin_file_path = os.path.join(plugin_path, "repo")
        if os.path.isdir(plugin_file_path) and os.path.isdir(packag_path):
            shutil.copytree(plugin_file_path, packag_path, dirs_exist_ok=True)

        sdk_aar_path = os.path.join(
            sdk_project_path, args.project_name+f"/build/outputs/aar/{args.project_name}-release.aar")
        new_aar_name = args.project_name+".aar"
        new_aar_path = os.path.join(packag_path, new_aar_name)
        if os.path.isfile(sdk_aar_path):
            shutil.copy(sdk_aar_path, new_aar_path)

        projects = ["demoAndroidX", "demo"]
        for projec_name in projects:
            project_path = os.path.join(sdk_project_path, projec_name)
            if os.path.isdir(project_path):
                lib = os.path.join(project_path, 'libs')
                if os.path.exists(lib):
                    shutil.rmtree(lib)
                os.mkdir(lib)
                os.chdir(lib)
                os.symlink(
                    "../../"+os.path.join(os.path.basename(packag_path), new_aar_name), new_aar_name)
        os.chdir(current_path)
        new_sdk_project_name = "new_sdk_project"
        new_sdk_project = os.path.join(current_path, new_sdk_project_name)
        shutil.copytree(sdk_project_path, new_sdk_project,
                        dirs_exist_ok=True, symlinks=True, ignore_dangling_symlinks=True)
        rm_files = ['.git', ".gradle", ".idea",
                    args.project_name, "build", os.path.basename(__file__), ".DS_Store", ".gitignore"]
        file_path = new_sdk_project

        def del_dir(delPath):
            if os.path.isdir(delPath):
                if os.path.basename(delPath) in rm_files:
                    shutil.rmtree(delPath,
                                  ignore_errors=True)
                else:
                    for item in os.listdir(delPath):
                        del_dir(os.path.join(delPath, item))
            elif os.path.isfile(delPath):
                if os.path.basename(delPath) in rm_files:
                    os.remove(delPath)

        del_dir(new_sdk_project)

        os.chdir(current_path)
        os.system('zip -r {} {}'.format(new_sdk_project_name +
                                        ".zip", new_sdk_project_name))
        shutil.rmtree(new_sdk_project, ignore_errors=True)
        print(f"完成任务 ✅✅✅✅✅✅")


def shell_script_execute(current_path, cmd):
    os.chdir(current_path)
    p = subprocess.Popen(cmd, shell=True)
    p.wait()
    if p.returncode == 0:
        print(f"完成任务 {cmd}")
        return True
    print(f"未完成任务 {cmd} ❌❌❌❌❌❌❌❌❌❌❌")
    return False


# 操作哪些根目录文件夹
def option_folder():
    sdk_name = os.path.basename(args.path)
    base_path = os.path.dirname(args.path)
    plugin_name = args.pluginname if args.pluginname else sdk_name+"-plugin"
    try:
        plugin_name_path = os.path.join(base_path, plugin_name)
        if os.path.exists(plugin_name_path) is False:
            plugin_name = sdk_name+'-plugin2'
            plugin_name_path = os.path.join(base_path, plugin_name)
        if os.path.exists(plugin_name_path):
            return [plugin_name, sdk_name]
        else:
            raise NameError
    except TypeError:
        print(f'{plugin_name}文件夹不存在')


# 参数配置
def get_parser():
    parser = argparse.ArgumentParser(description="""使用说明:\
        1. --path or -p 文件跟路径\
        2. --h5 or -h5 set value 0 or 1 匹配sa\
        3. --name or -n 自定义名称\
        4. --packag or -pg 打包demo""")

    parser.add_argument(
        '--path', '-p',
        dest='path',
        default=os.path.abspath(os.path.dirname(os.path.abspath(__file__))),
        # default='/Users/mac/Desktop/android_test',
        help="项目路径")
    parser.add_argument(
        '--h5', '-h5',
        dest='h5',
        type=int,
        default=0,
        help="是否恢复Sa default=0设置卓尔. 1是设置SA.")
    parser.add_argument(
        '--name', '-n',
        dest='project_name',
        default="ZallDataSDK",
        help="新SDK名称")
    parser.add_argument(
        '--packag', '-pg',
        dest='packag',
        default=0, help="打包SDK")
    parser.add_argument(
        '--plugin', '-pl',
        dest='plugin',
        default=1, help="打包plugin SDK")
    parser.add_argument(
        '--pluginname', '-pn',
        dest='pluginname',
        default=None, help="同目录SDK plugin 名称")

    return parser


args = get_parser().parse_args()
last_package = False
if __name__ == '__main__':
    path = os.path.dirname(args.path)
    plugin_names = option_folder()

    for index, name in enumerate(plugin_names):
        paths = dir_option(os.path.join(path, name))
        file_option(paths, plugin=(index == 0))
    auto_packag()
