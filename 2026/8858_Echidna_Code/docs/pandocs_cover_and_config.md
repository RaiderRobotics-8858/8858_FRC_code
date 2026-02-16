---
title: "Echidna Specifications"
subtitle: "2026 Robot of FRC Team 8858 \"Beast From the East\""
subject: "Markdown"
keywords: [Markdown]
lang: "en"
titlepage: true
classoption: [titlepage, twoside]
titlepage-text-color: "7137C8"
titlepage-rule-color: "7137C8"
titlepage-rule-height: 2
titlepage-logo: "../../../images/logos/RR&E_logo.png"
toc: true
toc-own-page: true
numbersections: true
pdf-engine: xelatex
mainfont: "segoeuithis"
mainfontoptions: [Path=fonts/, Extension=.ttf, BoldFont=segoeuithibd, ItalicFont=segoeuithisi, BoldItalicFont=segoeuithisz]
header-includes: |
  <!-- markdownlint-disable MD034 -->
  \usepackage{caption}
  \captionsetup[table]{position=above}
  \usepackage{hyperref}
  \usepackage[margin=0.75in]{geometry}
  \usepackage{float}
  \floatplacement{figure}{H}
  \usepackage{etoolbox}
  \usepackage{graphicx}
  \setkeys{Gin}{width=\linewidth,height=0.4\textheight,keepaspectratio}
  \makeatletter
  \newcommand{\twodigits}[1]{\ifnum#1<10 0\fi\number#1}
  \newfontfamily\titlefont[Path=fonts/]{mandalorexpandital.ttf}
  \renewcommand\section{\@startsection{section}{1}{\z@}{-3.5ex\@plus -1ex \@minus -.2ex}{2.3ex\@plus .2ex}{\Large\bfseries}}
  \providecommand{\subtitle}[1]{\gdef\@subtitle{#1}}
  \renewcommand{\maketitle}{%
    \begin{titlepage}
      \centering
  \vspace*{0.18\textheight}
      {\titlefont\Huge \@title\par}
      \ifdefined\@subtitle
        \vspace{3em}
        \includegraphics[width=0.7\textwidth]{\detokenize{../../../images/logos/RR&E_logo.png}}\par
        \vspace{3em}
  {\titlefont\Large \@subtitle\par}
    \vspace{1.5em}
  {\titlefont\large \twodigits{\month}/\twodigits{\day}/\number\year\par}
      \fi
      \vfill
    \end{titlepage}
  }
  \makeatother
  \usepackage{fancyhdr}
  \pagestyle{fancy}
  \fancyhf{}
  \fancyfoot[C]{\raisebox{0.20in}{\ifodd\value{page}{\href{\detokenize{https://www.thebluealliance.com/team/8858/}}{\titlefont 8858 'Beast From the East'}}\else\href{\#toc}{\includegraphics[height=0.6in]{\detokenize{../../../images/logos/RR&E_logo.png}}}\fi}}
  \fancyfoot[L]{\raisebox{0.36in}{\llap{{\titlefont\Huge\strut\ifodd\value{page}\else\thepage\fi}}}}
  \fancyfoot[R]{\raisebox{0.20in}{\rlap{{\titlefont\Huge\strut\ifodd\value{page}\thepage\fi}}}}
  \renewcommand{\headrulewidth}{0pt}
  \renewcommand{\footrulewidth}{0pt}
  \AtBeginDocument{\hypertarget{toc}{}}
  \pretocmd{\tableofcontents}{\pagestyle{empty}}{}{}
  \apptocmd{\tableofcontents}{\thispagestyle{empty}\pagestyle{fancy}}{}{}
  \pretocmd{\listoffigures}{\pagestyle{empty}}{}{}
  \apptocmd{\listoffigures}{\thispagestyle{empty}\pagestyle{fancy}}{}{}
  \pretocmd{\listoftables}{\pagestyle{empty}}{}{}
  \apptocmd{\listoftables}{\thispagestyle{empty}\pagestyle{fancy}}{}{}
  <!-- markdownlint-enable MD034 -->
footer-center: "Team 8858 'Beast From the East'"
...
