<html>
<head>
<meta name="layout" content="test"></meta>

<script>
		$(function () {
			$("#demo1")
				.jstree({
					"json_data" : { "ajax" : { "url" : "<g:createLink controller='entity' action='jstree'/>"}},
					"plugins" : [ "themes", "json_data", "ui" ]
				})
				.bind("select_node.jstree", function (event, data) {
					 alert(data.rslt.obj.attr("id"));
				});

		});

		$(function() {
			$( "#tabs" ).tabs();
		});
	</script>

	<script>
	$(function() {
		// setup ul.tabs to work as tabs for each div directly under div.panes
		$("ul.tabs").tabs("div.panes > div");
	});
	</script>
</head>
  <body>
    <!-- TABS CONTAINER -->
    <div class="ui-layout-center">
       <ul class="tabs">
			<li><a href="#">Summary</a></li>
			<li><a href="#">Locations</a></li>
			<li><a href="#">Health</a></li>
			<li><a href="#">Structure</a></li>
	   </ul>
	   <div class="panes">
			<div id ="entityName">First tab content. Tab contents are called "panes"</div>
			<div>Second tab content</div>
			<div>Third tab content</div>
			<div>Fourth tab content</div>
	   </div>
    </div>
    <!-- TREE CONTAINER -->
	<div class="ui-layout-west">
	<!-- This is the tree -->
		<div id="demo1"></div>
	</div>
  </body>
</html>