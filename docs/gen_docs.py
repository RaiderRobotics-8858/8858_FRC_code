import markdown
from weasyprint import HTML
import re
import os
import argparse
import fitz  # PyMuPDF
from pathlib import Path
from bs4 import BeautifulSoup, NavigableString, Comment
import tempfile
import shutil

class CustomHelpFormatter(
    argparse.RawDescriptionHelpFormatter,
    argparse.ArgumentDefaultsHelpFormatter,
    argparse.MetavarTypeHelpFormatter):
    pass

def extract_headings_with_numbers(markdown_content):
    """
    Extract headings from the Markdown content and assign hierarchical numbers.
    Returns a list of tuples (level, number, title, anchor).
    """
    headings = []
    counters = []  # To track numbering for each level

    for line in markdown_content.splitlines():
        match = re.match(r'^(#{1,6})\s+(.*)', line)
        if match:
            level = len(match.group(1))  # Number of '#' indicates the level
            title = match.group(2).strip()

            # Adjust counters for the current level
            while len(counters) < level:
                counters.append(0)
            while len(counters) > level:
                counters.pop()
            if counters:
                counters[-1] += 1
            else:
                counters.append(1)

            # Generate the hierarchical number
            number = '.'.join(map(str, counters))

            # Generate unique anchor using number and title
            anchor = f"{number}-{title}".lower().replace(' ', '-').replace('.', '').replace('#', '')

            headings.append((level, number, title, anchor))
    return headings

def extract_figures(html_content):
    """
    Extract figures (tables and images) from the HTML content and assign numbers.
    Returns a list of tuples (type, number, caption).
    """
    figures = []
    table_count = 0
    image_count = 0
    soup = BeautifulSoup(html_content, 'html.parser')
    table_captions = {}
    # Assign ids to tables first
    for idx, table in enumerate(soup.find_all('table'), 1):
        table['id'] = f"table-{idx}"
    # Now associate comments to table ids
    comments = [elem for elem in soup.descendants if isinstance(elem, Comment)]
    for comment in comments:
        if 'table:' in comment.string:
            match = re.search(r'table:\s*(.+)', comment.string.strip())
            if match:
                caption = match.group(1).strip()
                # Find the next table after this comment
                next_table = comment.find_next('table')
                if next_table and next_table.has_attr('id'):
                    table_captions[next_table['id']] = caption
                comment.extract()
    for table in soup.find_all('table'):
        table_count += 1
        tid = table.get('id', f"table-{table_count}")
        caption = table_captions.get(tid, f"Table {table_count}")
        figures.append(('table', table_count, caption))
        # Add caption below the table
        caption_tag = soup.new_tag('p')
        caption_tag['style'] = 'text-align:center;font-style:italic;font-size:0.95em;margin-top:0.2em;'
        caption_tag.string = f"Table {table_count}: {caption}"
        table.insert_before(caption_tag)
    for img in soup.find_all('img'):
        image_count += 1
        img['id'] = f"figure-{image_count}"
        caption = img.get('alt', f"Figure {image_count}")
        figures.append(('figure', image_count, caption))
        # Add caption below the image
        caption_tag = soup.new_tag('p')
        caption_tag['style'] = 'text-align:center;font-style:italic;font-size:0.95em;margin-top:0.2em;'
        caption_tag.string = f"Figure {image_count}: {caption}"
        img.insert_after(caption_tag)
    # Update the html_content to remove the comments
    html_content = str(soup)
    return figures, html_content

def generate_lof(figures):
    """
    Generate HTML for the List of Figures.
    """
    lof_html = '<div id="lof"><h1>List of Figures</h1>'
    for fig_type, num, caption in figures:
        link_type = 'table' if fig_type == 'table' else 'figure'
        lof_html += f'<p><a href="#{link_type}-{num}">{fig_type.capitalize()} {num}: {caption}</a></p>'
    lof_html += '</div>'
    return lof_html

def generate_toc(headings):
    """
    Generate an HTML Table of Contents from the extracted headings.
    """
    def toc_html_with_pages(page_numbers=None):
        toc_html = '<div id="toc"><h1>Table of Contents</h1><table style="width:100%;border-collapse:collapse;">\n<tr><th style="text-align:left;">Section</th><th style="text-align:left;">Title</th><th style="text-align:right;width:60px;">Page</th></tr>'
        for level, number, title, anchor in headings:
            heading_text = f"{number} {title}"
            page_num = page_numbers.get(heading_text, "") if page_numbers else ""
            toc_html += f'''<tr>
                                <td style="padding-left:{20 * (level - 1)}px;text-align:left;white-space:nowrap;margin-right:20px;">{number}</td>
                                <td colspan="2" style="padding:0;">
                                    <div style="display:flex;flex-direction:row;align-items:center;">
                                        <span style="text-align:left;white-space:nowrap;"><a href="#{anchor}" style="color:inherit;text-decoration:none;">{title}</a></span>
                                        <span style="flex:1 1 auto;letter-spacing:2px;overflow:hidden;">{'.'*60}</span>
                                        <span style="text-align:right;min-width:40px;white-space:nowrap;">{page_num}</span>
                                    </div>
                                </td>
                        </tr>'''
        toc_html += '</table></div>'
        return toc_html
    return toc_html_with_pages

def convert_markdown_to_pdf(md_file_paths, output_pdf_path):
    # Read the Markdown file
    markdown_content = "" # Initialize empty content
    for input_md_path in md_file_paths:
        print(f"Reading Markdown file: {input_md_path}")
        with open(input_md_path, 'r', encoding='utf-8') as md_file:
            markdown_content = markdown_content + md_file.read()

    # Replace pagebreak comments with HTML page breaks
    markdown_content = markdown_content.replace('<!-- pagebreak -->', '<div style="page-break-before: always;"></div>')

    # Extract headings with numbers for the Table of Contents
    headings = extract_headings_with_numbers(markdown_content)

    # First pass: generate PDF without page numbers
    toc_html_func = generate_toc(headings)
    toc_html = toc_html_func({})

    html_content = markdown.markdown(markdown_content, extensions=['fenced_code', 'codehilite', 'tables'])

    # Add class to markdown tables
    soup_temp = BeautifulSoup(html_content, 'html.parser')
    for table in soup_temp.find_all('table'):
        table['class'] = table.get('class', []) + ['markdown-table']
    html_content = str(soup_temp)

    figures, html_content = extract_figures(html_content)
    lof_html = generate_lof(figures)

    # Add ids and numbers to headings
    soup = BeautifulSoup(html_content, 'html.parser')
    heading_tags = soup.find_all(['h1', 'h2', 'h3', 'h4', 'h5', 'h6'])
    for i, tag in enumerate(heading_tags):
        if i < len(headings):
            level, number, title, anchor = headings[i]
            tag['id'] = anchor
            # Prepend number
            if tag.contents:
                first = tag.contents[0]
                if isinstance(first, NavigableString):
                    first.replace_with(f"{number} {first}")
                else:
                    tag.insert(0, NavigableString(f"{number} "))
            else:
                tag.string = f"{number} "
    html_content = str(soup)

    # Add TOC before first heading
    first_heading_match = re.search(r'<h[1-6][^>]*>.*?</h[1-6]>', html_content)
    if first_heading_match:
        html_content = toc_html + '<div style="page-break-before: always;"></div>' + lof_html + '<div style="page-break-before: always;"></div>' + html_content

    style = """
    <style>
        body {
            font-family: 'Segoe UI', 'Arial', sans-serif;
        }
        img {
            max-width: 100%;
            height: auto;
            max-height: 90vh;
            display: block;
            margin: 0.5em auto;
        }
        table {
            border-collapse: collapse;
            width: 100%;
        }
        .markdown-table th, .markdown-table td {
            border: 1px solid black;
            padding: 8px;
            text-align: left;
        }
        @page {
            @bottom-center {
                content: counter(page);
                font-size: 12px;
                color: #888;
            }
        }
    </style>
    """
    full_html = style + html_content
    base_url = str(Path(md_file_paths[0]).parent.resolve())
    HTML(string=full_html, base_url=base_url).write_pdf(output_pdf_path)
    print(f"First pass PDF generated: {output_pdf_path}")

    # Second pass: extract page numbers and regenerate PDF
    doc = fitz.open(output_pdf_path)
    heading_pages = {}
    for page_num in range(len(doc)):
        page = doc[page_num]
        text = page.get_text()
        for level, number, title, anchor in headings:
            heading_text = f"{number} {title}"
            if heading_text in text:
                heading_pages[heading_text] = page_num + 1
    doc.close()
    print(f"Extracted page numbers: {heading_pages}")

    # Regenerate TOC with page numbers
    toc_html = toc_html_func(heading_pages)
    html_content = markdown.markdown(markdown_content, extensions=['fenced_code', 'codehilite', 'tables'])
    # Add class to markdown tables
    soup_temp = BeautifulSoup(html_content, 'html.parser')
    for table in soup_temp.find_all('table'):
        table['class'] = table.get('class', []) + ['markdown-table']
    html_content = str(soup_temp)

    # Add ids and numbers again
    soup = BeautifulSoup(html_content, 'html.parser')
    heading_tags = soup.find_all(['h1', 'h2', 'h3', 'h4', 'h5', 'h6'])
    for i, tag in enumerate(heading_tags):
        if i < len(headings):
            level, number, title, anchor = headings[i]
            tag['id'] = anchor
            # Prepend number
            if tag.contents:
                first = tag.contents[0]
                if isinstance(first, NavigableString):
                    first.replace_with(f"{number} {first}")
                else:
                    tag.insert(0, NavigableString(f"{number} "))
            else:
                tag.string = f"{number} "
    html_content = str(soup)

    figures, html_content = extract_figures(html_content)
    lof_html = generate_lof(figures)

    if first_heading_match:
        html_content = toc_html + '<div style="page-break-before: always;"></div>' + lof_html + '<div style="page-break-before: always;"></div>' + html_content
    full_html = style + html_content
    HTML(string=full_html, base_url=base_url).write_pdf(output_pdf_path)
    print(f"Final PDF generated: {output_pdf_path}")

def main():
    """
    Main function to execute the script.
    """
    # format the parser...
    parser = argparse.ArgumentParser(
        formatter_class = CustomHelpFormatter
    )

    parser.add_argument("-f", "--files", dest="files", type=str, nargs='+', help="Markdown files to convert", required=True)
    parser.add_argument("-o", "--outfile", dest="outfile", type=str, help="Output PDF file", default="output.pdf")
    args = parser.parse_args()

    md_file_paths = args.files
    output_pdf_path = args.outfile

    # Convert the Markdown files to PDF
    convert_markdown_to_pdf(md_file_paths, output_pdf_path)

if __name__ == "__main__":
    main()