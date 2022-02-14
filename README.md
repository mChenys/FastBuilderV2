
# FastBuilder
[![versionImg](https://badgen.net/maven/v/metadata-url/https://plugins.gradle.org/m2/io/github/tiyateam/fastbuilder/FastBuilder/maven-metadata.xml?label=FastBuilder)](https://plugins.gradle.org/plugin/io.github.tiyateam.fastbuilder)

一个可以提升`Android`编译效率的Gradle插件


# 编译时间对比
集成插件前:

![image](https://user-images.githubusercontent.com/22413240/153822101-c7933143-5c7d-4a93-9317-e5a294da4e2f.png)

集成插件后,且存在aar:

![image](https://user-images.githubusercontent.com/22413240/153822135-ed9f550c-49ed-44c6-802b-db191042cf34.png)


# 插件原理

针对业务构建自定义增量条件

[Android 编译优化探索](https://fanmingyi.blog.csdn.net/article/details/122638149)

[Android 编译优化探索2 Hack字节码](https://fanmingyi.blog.csdn.net/article/details/122760183)



# 使用指南

- (1)在您的根目录的下`build.gradle`启用插件


```groovy
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "io.github.tiyateam.fastbuilder:FastBuilder:2.0.12"
  }
}

apply plugin: "io.github.tiyateam.fastHackPlugin"
```

- (2)在您的`App`工程下的`build.gradle`启用另一个插件

```groovy
apply plugin: "io.github.tiyateam.fastbuilder"
//进行相关配置
moduleArchive {
    //是否开启日志
    logEnable = true
    //是否启用
    pluginEnable = true
    //哪些模块参与优化
    subModuleConfig {
        register(":image-picker") {
        }
        register(":floatwindow") {
        }
        register(":common") {
        }
        register(":live") {
        }
        register(":login_tiya") {
        }
        register(":pay-tiya") {
        }
        register(":pair") {
        }
        register(":pay-tiya-google") {
        }
        register(":player") {
        }
        register(":pushpermission") {
        }
        register(":record") {
        }
        register(":share-tiya") {
        }
        register(":ucrop") {
        }
        register(":videoprocessor") {
        }
        register(":live-tiya-rtc") {
        }
        register(":im") {
        }
        register(":audio_mixer") {
        }
        register(":analysis-tiya") {
        }
        register(":agent") {
        }
        register(":banner") {
        }
        register(":ImagePreview") {
        }
        register(":indexablerecyclerview") {
        }
        register(":cardstackview") {
        }
        register(":user") {
        }
        register(":base") {
        }
        register(":xhook_commom") {
        }
        register(":pthread_hook") {
        }



    }
}
```

# FAQ
1. 编译异常
![image](https://user-images.githubusercontent.com/22413240/153822476-fed811f2-e396-4ef5-8875-3fd85ce7dfd0.png)
 
执行`./gradlew --stop`即可。此命令作用为杀死守护进程，因为此刻守护进程已经加载了目标类，导致插件无法加载相同限定名的hook类


# 谁在使用

| [TIYA](https://play.google.com/store/apps/details?id=com.huanliao.tiya&hl=en_US&gl=US)        | 
| --------   | 
|[<img src="https://play-lh.googleusercontent.com/RwuBOgoBX1OmmR5W14AyBDp9pNgnh1eJD2UmJzhVSZOpZYG1xI_y1aihbE4aP3dURwc=s360-rw" alt="TIYA" width="150"/> ](https://play.google.com/store/apps/details?id=com.huanliao.tiya&hl=en_US&gl=US)       |


# License
```
Copyright 2021 Tiya.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

# BUG
1. 找到布局资源
2. 再配合kotlin-synthetic改变view出错
