:page/title "Untitled"
:page/description ""
:page/date ""
:blog-post/tags nil
:blog-post/author {:person/id :jan}
:page/body
---
title: 博客使用的技术栈
author: linzihao
date: "2024-07-10"
description: An overview of the technologies and tools used to create this blog, including Astro, MDX, React, Tailwind CSS, and more.
lang: "zh"
tags: ["astro"]
---
用的是 astro

## mdx

一个命令
`pnpm astro add mdx`
就可以在 astro 项目里面加上 mdx 支持了, 会自动加依赖, 并且修改 astro 配置文件, 把插件加上.

## react
`pnpm astro add react`
加上 react, 会自动添加依赖, 修改 tsconfig, 修改 astro 配置

## tailwind and style
`pnpm astro add tailwind`

其他 UI 相关的依赖
`pnpm i @headlessui/react`
`pnpm i @heroicons/react@v1`


`pnpm install -D @tailwindcss/typography`
然后在 tailwind.config.js 里面修改
```javascript
plugins: [
    require('@tailwindcss/typography'),
    // ...
],
```
现在多了一个 utility class prose 可以用了

图片用了 astro 的 Image component 做自动优化, 因为用的是 pnpm, 不会自动安装额外的依赖, 需要手动装一下
`pnpm add sharp`

## google analytic
`pnpm astro add partytown`
需要用到 partytown, partytown 是用来懒加载第三方 script 的, 可以优化加载速度

去 google analytic 创建个媒体资源, 会拿到对应的 gtag id. 
替换到 GoogleAnalytic component 就可以
```astro
<!-- Google tag (gtag.js) -->
<script
  type="text/partytown"
  async
  src="https://www.googletagmanager.com/gtag/js?id=tag"></script>
<script type="text/partytown">
  window.dataLayer = window.dataLayer || [];
  function gtag() {
    dataLayer.push(arguments);
  }
  gtag("js", new Date());

  gtag("config", "tag");
</script>

```


## 评论系统
评论用的是 [giscus](https://giscus.app/zh-CN)
在 github 仓库里面 enable discussion 功能, 把仓库设置成 public 的, 否则看不到评论.
在 giscus 页面里面填仓库地址, 设置一下, 就会拿到一串 script, 把这个做成 astro 的 component 再引用到 blog layout 里面
```astro
<section class="giscus mx-auto mt-10 w-full"></section>

<script src="https://giscus.app/client.js"
        data-repo="schneiderlin/astro_blog"
        data-repo-id="R_kgDOMUUSTw"
        data-category="Blog Post Comments"
        data-category-id="DIC_kwDOMUUST84Cgr-R"
        data-mapping="url"
        data-strict="0"
        data-reactions-enabled="1"
        data-emit-metadata="0"
        data-input-position="bottom"
        data-theme="preferred_color_scheme"
        data-lang="zh-CN"
        data-loading="lazy"
        crossorigin="anonymous"
        async>
</script>
```

## 阅读进度条
astro component 里面可以直接写 `<script>` 和 `<style>` tag, 因此一个 component 就可以实现进度条功能
```astro
---
---
<div id="progress-bar">

</div>
<script>
    function updateProgressBar() {
        const {scrollTop, scrollHeight} = document.documentElement
        const scrollPercent = `${(scrollTop / (scrollHeight - window.innerHeight)) * 100}%`

        document.querySelector('#progress-bar')!.style.setProperty('--progress', scrollPercent)
    }

    document.addEventListener('scroll', updateProgressBar)
</script>

<style>
    #progress-bar {
        --progress: 0;
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        height: 6px;
        width: var(--progress);
        background-color: #502020;
    }
</style>
```

## sitemap
`pnpm astro add sitemap`
需要在 astro.config.js 里面写 site
```javascript
import { defineConfig } from 'astro/config';
import sitemap from '@astrojs/sitemap';

export default defineConfig({
  // ...
  site: 'https://stargazers.club',
  integrations: [sitemap()],
});
```

再 npx astro build, 会打印出在哪个路径输出了 sitemap xml, 这一步只是为了检查
在 layout 里面把 sitemap 加到 head 里面
```astro
<head>
  ...
  <link rel="sitemap" href="/sitemap-index.xml" />
</head>
```

## Open Graph
Open Graph 是用来在社交网络上分享链接时显示的图片和描述, 用的是 [ogp.me](https://ogp.me/) 的规范
在 https://www.opengraph.xyz/ 输入 url 可以预览这个 url 在各个社交网络上的分享效果

不需要用到任何外部的工具, 每个 blog post 的 frontmatter 里面可以自己加 ogImage 和 description.
写一个 OpenGraphTags 的 component, 在 Layout 里面引用

```astro
---
interface Props {
  title: string;
  description: string;
  ogImage?: string;
}

const { title, description, ogImage } = Astro.props;
const siteUrl = Astro.site ? Astro.site.href : 'https://linzihao.com/';
---

<!-- Open Graph / Facebook -->
<meta property="og:type" content="article">
<meta property="og:url" content={Astro.url}>
<meta property="og:title" content={title}>
<meta property="og:description" content={description}>
{ogImage && <meta property="og:image" content={new URL(ogImage, siteUrl)}>}

<!-- Twitter -->
<meta property="twitter:card" content="summary_large_image">
<meta property="twitter:url" content={Astro.url}>
<meta property="twitter:title" content={title}>
<meta property="twitter:description" content={description}>
{ogImage && <meta property="twitter:image" content={new URL(ogImage, siteUrl)}>}
```

在 Layout 里面使用
```astro
<OpenGraphTags title={frontmatter.title} description={frontmatter.description} ogImage={frontmatter.ogImage} />
```

## Icon
用的是 [Iconify](https://iconify.design/) 的图标, 在 astro 里面用的是 [iconify/astro](https://github.com/iconify/astro)

安装
`pnpm astro add astro-icon`

在 Iconify 里面搜索图标, 拿到图标分两部分, icon set 和 icon name, 例如 `mdi:home`, mdi 是 icon set, home 是 icon name.
使用 icon 的 component 是 
```astro
import { Icon } from 'astro-icon/components';

<Icon icon="icon-set:icon-name" />
```

如果提示找不到 icon-set, 需要安装对应的 icon set, 例如
`pnpm add @iconify-json/simple-icons`

## 全文搜索
用 PageFind 是在 build 的时候, 指定文件夹, 把文件夹里面的所有指定文件扫描, 并且生成对应的索引和搜索组件

build step
```json
{
  "scripts": {
    "postbuild": "pagefind --site dist",
  }
}
```

在页面中使用
```astro
<link href="/pagefind/pagefind-ui.css" rel="stylesheet" />
<script is:inline src="/pagefind/pagefind-ui.js"></script>

<div id="search"></div>
<script>
  window.addEventListener('DOMContentLoaded', (event) => {
    new (window as any).PagefindUI({ 
      element: '#search', 
      processTerm: (term: string) => {
        return term; // 这里可以帮用户分词
      },
      showSubResults: true 
    });
  });
</script>
```

PageFind 会根据 html 的 lang 属性, 生成对应的语言的搜索组件.
因此在每个 blog 的 fontmatter 里面都设置了 lang, 并且在传递到 Layout 中.